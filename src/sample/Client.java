package sample;

import java.io.OutputStream;
import java.net.Socket;

public class Client {

  public static void main(String[] args) throws Exception {
    System.out.println("connecting to server");
    Socket socket = new Socket("localhost", 8080);
    System.out.println("connected; sending data");
    OutputStream out = socket.getOutputStream();
    int byteToSend = 42;
    out.write(byteToSend);
    System.out.println("sent " + byteToSend);
    out.close();
    socket.close();
    System.out.println("cleaned up");
  }
}
