# TCP sockets in Java

TCP sockets enable communication between programs within the same machine or across different machines.
Data can be sent/received using the regular Java OutputStream/InputStream.

```java
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
```java
int portNumber = 8080;
// allocate port on all available IP addresses
ServerSocket ss = new ServerSocket(portNumber);
// wait for an incoming connection
Socket socket = ss.accept();
// use connection
```

What's the difference between `Socket` and `ServerSocket`?
* A ServerSocket **holds a queue of incoming connections** on the specified port.
  It does not represent a connection.
* A Socket **represents a connection**.
  It is defined by its { local ip, local port, remote ip, remote port } quadruple.

Outgoing connections can be created in Java using the `Socket` class:
```java
// connect to port 8080 of the local machine using the loopback address
Socket socket = new Socket("127.0.0.1", 8080);
// note that 8080 is the "remote port". local port number is chosen automatically (randomly)
// use the input/output streams here
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
The syntax describes how the data is encoded into message bytes.
Using a sane syntax makes it easy to read and decode the received message bytes.

### Example syntax

Here's an example of network communication between a service that can register students to courses and a client.
It uses the request-response pattern and the type-length-value (TLV) style syntax:
* first byte of a message defines the message type
* second byte is the message length *N*
* next *N* bytes are the message value

1) the client sends a byte array `[1,4,155,141,162,164]`.
   the type is set to 1 (new registration).
   the message length is set to 4.
   the following next 4 bytes contain the UTF-8 encoded string "mart" as the message value.
2) the server responds with bytes `[2,0]`, where the type is set to 2 (registration ok) and message length is set to zero.
   alternatively the server could respond with `[3,228,..]`, where the type is set to 3 (registration error), length is set to 228 and the next 228 bytes contain an UTF-8 encoded error string.
3) the client sends a new message..

The main thing to consider when designing a syntax is to make it easy to decode.
The decoder should never guess the size of the message or read until no data is left in the input stream.
Sending the message type and length before the message helps with that.

### Avoid messages without syntax

The most common pitfall is to try to write a chat program that just reads a string from `System.in` and writes it to a network stream.
The other side of the connection reads messages and prints them out.
At the same time it also reads strings from `System.in` and sends them back to the other.

It's a simple program and it kind of works.
The problems quickly become apparent when the program must be modifed to do anything non-trivial, e.g. transfer a file.
If the sender just dumps the file content to the network stream, then the receiver has no way to differentiate it from a regular text message.
Receiving a confirmation of successful transfer from the destination is even harder.
Again there is no way to differentiate the confirmation message from a regular text message (or a file transfer started from the other side).
Using the request-response pattern and a proper syntax would resolve both issues.

### Use data streams

Java has the DataOutputStream and DataInputStream classes.
Use the writeInt/readInt methods to send integers - the methods always write/read 4 bytes which makes it super useful for implementing the type/length syntax.
The writeUTF method can be used to send a string - it encodes the string into bytes and automatically writes the length of the string before the encoded bytes.
The readUTF method can use the same length prefix to know exactly how much to read.

The ByteArrayOutputStream class can be used to build the message value in memory before sending it out:
```
var baos = new ByteArrayOutputStream();
try (var out = new DataOutputStream(baos)) {
  out.writeInt(123);
  out.writeUTF("important");
}
byte[] value = baos.toByteArray();
// send type, value.length, value
```

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
* consecutive groups of zeroes can be omitted, but only once in an address
* the address is often wrapped in brackets to avoid confusing the port number separator with the address group separators

For example, an URL containing the above address could be written as `https://[2001:db8::ff00:42:8329]:8443/`.
Note that the three groups of zeros have been omitted and all leading zeros have been removed.
The port number 8443 is placed outside the brackets.
