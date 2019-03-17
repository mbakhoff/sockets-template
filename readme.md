# TCP sockets in Java

TCP sockets enable communication between programs within the same machine or across different machines.
Data can be sent/received using the regular Java OutputStream/InputStream.

```
Socket s = connect();
InputStream in = s.getInputStream();
OutputStream out = s.getOutputStream();
// send and receive data from the connected program
```

## Estabilishing a connection

The connection is established between two programs.
One of the programs (the server) will wait for an incoming connection and accept it.
The other program (the client) will create an outgoing connection to the server.
To connect to the server, the client needs to know the server's **IP address** and **port number**.

An IP address uniquely identifies a machine in the network.
It is usually written as 4 dot-separated integers, e.g. `192.168.1.1`.
A machine can have several IP addresses if it has several network cards (some may be virtual).
Each machine also has a loopback IP address called *localhost*: `127.0.0.1`.
This can be used to refer to the local machine and make connections to it.

Each machine can have multiple programs running on it, each of them creating and accepting connections.
Port numbers are used to differentiate between different connections.
A program can claim any free port number for any local IP address and use it to accept and create connections.
The connections are identified by the quadruples of { local ip, local port, remote ip, remote port }.

One way to think about the IP addresses and ports is to use the apartment building analogy.
The IP address is the building's address.
The port number is the mailbox number in a building.
Different owners (programs) can own different mailboxes in a building (IP).
When a message arrives at some building (IP), the house keeper (operating system) will deliver the message to the right mailbox (program) by looking at the mailbox number (port number).

Incoming connections can be accepted in Java using the `ServerSocket` class:
```
int portNumber = 8080;
// allocate port on all available IP addresses
try (ServerSocket ss = new ServerSocket(portNumber)) {
  // wait for an incoming connection
  try (Socket socket = ss.accept()) {
    // use connection
  }
}
```

What's the difference between `Socket` and `ServerSocket`?
* A ServerSocket **holds a queue of incoming connections** on the specified port.
  It does not represent a connection.
* A Socket **represents a connection**.
  It is defined by its { local ip, local port, remote ip, remote port } quadruple.

Outgoing connections can be created in Java using the `Socket` class:
```
// connect to port 8080 of the local machine using the loopback address
// note that 8080 is the "remote port". local port number is chosen automatically (randomly)
try (Socket socket = new Socket("localhost", 8080)) {
  // use the input/output streams here
}
```

## Tips and tricks

* This repository includes a runnable example.
  Start the server first.
  Start the client while the server is still running, otherwise there is nothing to accept the client's connection.
* It is possible to accept multiple connections from a single `ServerSocket`.
  Usually `accept()` is called in some sort of loop.
* Sockets accepted from the same ServerSocket use the same local port, but the remote ip/port are different.
* InputStream methods can block when trying to read more than the remote has sent.
  To see what the code is waiting for, use the "Thread dump" button in the IDE debugger panel or the *jstack* command line utility.
* Ports numbered 1-1024 are *privileged ports*, often used to run system services.
  Trying to use these can throw an exception unless the program is runnings with admin rights.
  Use a higher numbered port instead.
* You can check your machine's IP using these commands: `ip addr` (Linux), `ifconfig` (Mac) or `ipconfig` (Windows).
* Sometimes the local firewall configuration prevents accepting connections from remote machines (Windows or locked down Linux).
  Sometimes the router configuration prevents creating connections to other machines in the local network (e.g. university wifi).

## How to organize network communication

The most common way to communicate over the network is to use the [request-response pattern](https://en.wikipedia.org/wiki/Request%E2%80%93response).
The client sends a message to the server and the server sends a reply.
The client won't send a new message before receiving a reply for the previous one.
The server never sends any non-reply messages to the client.

Another important aspect in communication is the message syntax.
A socket only provides the input/output streams that transfer bytes.
The syntax describes how the data is encoded into bytes so it can be later reconstructed.

### Example syntax

Here's an example of network communication between a service that can register students to courses and a client.
The first byte of a message contains the message type.
The next bytes contain the message content depending on the message type.

1) the client sends a byte array `[1,4,155,141,162,164]`.
   the first byte shows the message type `1` for "new registration".
   the second byte `4` is the length of the student's name, followed by 4 bytes `[155,141,162,164]` for a UTF-8 encoded string "mart".
2) the server responds with a single byte `[2]` - the message type for "registration ok".
   alternatively the server could respond with `[3,155,..]`, where the type `3` is for "registration error", followed by `155` for error length, followed by 155 bytes containing the bytes for the error string.
3) the client sends a new message..

The key to a good syntax is to add a length prefix to every variable length piece of data.
Otherwise the receiver will have no way of knowing how much it needs to read.

### Using data streams

Java has the DataOutputStream and DataInputStream classes with many useful helper methods.
Make use of them!

Sending strings is one of the most common operation.
Let's take a look on how it could be done using data streams.

```
void writeStrings(DataOutputStream out) throws Exception {
  // bad example; don't do this!
  byte[] m1bytes = "message1".getBytes("UTF-8");
  byte[] m2bytes = "message2".getBytes("UTF-8");
  out.write(m1bytes);
  out.write(m2bytes);
}
```

This code successfully sends two strings to the output stream.
However, how can the receiver reconstruct this data?
How could they find out where one string ends and another starts?
There is not way to make such approach work reliably.
The solution is to **send a length prefix** before each variable length piece of data.

```
void myWriteUTF(DataOutputStream out, String str) throws Exception {
  byte[] encoded = str.getBytes("UTF-8"); // always specify encoding
  out.writeInt(encoded.length); // int is always exactly 4 bytes
  out.write(encoded);
}

String myReadUTF(DataInputStream in) throws Exception {
  int length = in.readInt(); // read exactly 4 bytes
  byte[] encoded = in.readNBytes(length); // useful method!
  return new String(encoded, "UTF-8");
}
```

The above methods are so commonly needed that they were built into the data stream classes.
The above could be replaced with simply `out.writeUTF(str)` and `in.readUTF()`.

Datastreams make it easy to write more complex messages as well.
Sending a request to get a student's grade for a course:
```
static final int TYPE_GET_GRADE = 4;

void sendGetGradeRequest(DataOutputStream out, String studentId, int courseId) throws Exception {
  out.writeInt(TYPE_GET_GRADE);
  out.writeUTF(studentId);
  out.writeInt(courseId);
}
```

Reading the request on the server side:
```
static final int TYPE_GET_GRADE = 4;
static final int TYPE_GRADE_OK = 5;
static final int TYPE_GRADE_ERROR = 6;

void processRequest(DataInputStream in, DataOutputStream out) throws Exception {
  int type = in.readInt();
  if (type == TYPE_GET_GRADE) {
    processGetGrade(in, out);
  } else {
    throw new RuntimeException("unknown type " + type);
  }
}

void processGetGrade(DataInputStream in, DataOutputStream out) throws Exception {
  String studentId = in.readUTF();
  int courseId = in.readInt();
  if (hasCompletedCourse(studentId, courseId)) {
    out.writeInt(TYPE_GRADE_OK);
    out.writeDouble(getGrade(studentId, courseId));
  } else {
    out.writeInt(TYPE_GRADE_ERROR);
  }
}
```

A few issues to avoid:
* instead of `writeUTF(name + ";" + description)` use `writeUTF(name); writeUTF(description);`.
  this way there's no need split the strings manually and the code also works with names that include the `;` symbol.
* instead of `writeUTF(Integer.toString(123))` use `writeInt(123)`.
  always try to use the right type and avoid unnecessary string conversions.
* use `writeInt(123)` instead if `write(123)`.
  the parameter of `write` is an int (4 bytes), but it only sends a single byte (wtf java).

### Use xml/json for more complex data structures

When you need to send a more complicated object over the network, then manually encoding and decoding it to/from bytes can be quite annoying.
Encode the object into a string using [gson](https://github.com/google/gson/blob/master/UserGuide.md#TOC-Object-Examples), then send the string as the message value.
Gson can decode the string back into the object on the receiving side.
The string should still be wrapped with the regular type-length-value syntax.
Note that string encoded messages are very space-inefficient.

## Notes on IPv6

There are two formats of IP addresses: IPv4 and IPv6.
The old IPv4 addresses (e.g. `192.168.1.1`) are essentially 32-bit integers (the dot separated string is just a convenient notation).
There are more computers on the planet than there are 32-bit integers, which causes all sorts of problems.
The new IPv6 addresses are 128-bit integers that look something like `2001:0db8:0000:0000:0000:ff00:0042:8329` (8 groups of 16 bits each).
The world is slowly moving to use these instead of IPv4.

Some things to keep in mind when using IPv6 addresses:
* leading zeros from any group can be removed
* consecutive groups of zeroes can be omitted.
  this can only be done once in an address, otherwise it's impossible to reconstruct the address.
* the address is often wrapped in brackets to avoid confusing the port number separator with the address group separators (both use `:`)

For example, an URL containing the above address could be written as `https://[2001:db8::ff00:42:8329]:8443/`.
Note that the three groups of zeros have been omitted and all leading zeros have been removed.
The port number 8443 is placed outside the brackets.
