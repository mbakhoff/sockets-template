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
It is usually written as 4 integers in range 0-255, e.g. `192.168.1.1`.
A machine can have several IP addresses if it has several network cards.
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
When a message arrives at some building (IP), the house keeper (operating system) will deliver the message to the right mailbox (program) by looking at the mailbox number (port).

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
* A ServerSocket holds a **queue** of incoming connections on the specified port.
  It does not represent a connection.
* A Socket represents an actual connection.
  It is defined by its { local ip, local port, remote ip, remote port } quadruple.

Outgoing connections can be created in Java using the `Socket` class:
```java
// allocates a random local port
// connects to the local machine using the loopback address
Socket socket = new Socket("127.0.0.1", 8080);
// use connection
```

## Tips and tricks

* This repository includes a runnable example.
  Start the server first.
  Start the client while the server is still running, otherwise there is nothing to accept the client's connection.
* Don't just write strings into sockets.
  Avoid using OutputStreamWriter/BufferedWriter with sockets.
  The receiver should always be able to determine how much data needs to be read.
  Use DataOutputStream+writeUtf instead - this sends a 2-byte length prefix, followed by the string content.
* It is possible to accept multiple connections from a single `ServerSocket`.
  Usually `accept()` is called in some loop.
* Sockets accepted from the same ServerSocket use the same local port, but the remote ip/port are different.
* InputStream methods can block (wait/hang/sleep) when trying to read more than the remote has sent.
  To see what the code is waiting for, use the "Thread dump" button in the IDE debugger panel or the *jstack* command line utility.
* Ports numbered 1-1024 are *privileged ports*, often used to run system services.
  Trying to use these can throw an exception unless the program is runnings with admin rights.
  Use a higher numbered port instead.
* There are two formats of IP addresses: IPv4 and IPv6.
  The old IPv4 addresses are written as four groups of 0-255, but essentially they are just 32-bit integers.
  There are more computers on the planet than there are 32-bit integers, so things are kind of messed up.
  The IPv6 addresses are 128-bit integers that look something like `2001:db8::ff00:42:8329`.
  The world is slowly moving to use these instead of IPv4.
* You can check your machine's IP using the `ip addr` (Linux) or `ipconfig` (Windows) command.
  Sometimes the local firewall configuration prevents accepting connections from remote machines (Windows or locked down Linux).
  Sometimes the router configuration prevents creating connections to other machines in the local network (e.g. university wifi).
