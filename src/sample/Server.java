package sample;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

  public static void main(String[] args) throws Exception {
    ServerSocket ss = new ServerSocket(8080);
    System.out.println("now listening on :8080");
    Socket socket = ss.accept();
    System.out.println("client connected; waiting for a byte");
    InputStream in = socket.getInputStream();
    int receivedByte = in.read();
    System.out.println("received " + receivedByte);
    in.close();
    socket.close();
    ss.close();
    System.out.println("cleaned up");
  }
}
