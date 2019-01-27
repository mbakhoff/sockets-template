package sample;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

  public static void main(String[] args) throws Exception {
    try (ServerSocket ss = new ServerSocket(8080)) {
      System.out.println("now listening on :8080");
      try (Socket socket = ss.accept();
           InputStream in = socket.getInputStream()) {
        System.out.println("client connected; waiting for a byte");
        int receivedByte = in.read();
        System.out.println("received " + receivedByte);
      }
    }
    System.out.println("finished");
  }
}
