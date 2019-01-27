package sample;

import java.io.OutputStream;
import java.net.Socket;

public class Client {

  public static void main(String[] args) throws Exception {
    System.out.println("connecting to server");
    try (Socket socket = new Socket("localhost", 8080);
         OutputStream out = socket.getOutputStream()) {
      System.out.println("connected; sending data");
      int byteToSend = 42;
      out.write(byteToSend);
      System.out.println("sent " + byteToSend);
    }
    System.out.println("finished");
  }
}
