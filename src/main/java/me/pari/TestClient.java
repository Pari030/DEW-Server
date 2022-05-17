package me.pari;

import java.io.PrintWriter;
import java.net.Socket;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;

public class TestClient {

    public static void main(String[] args) {

        try (Socket socket = new Socket("127.0.0.1", 7777)) {
            Thread.sleep(1000);
            DataOutputStream writer = new DataOutputStream(socket.getOutputStream());
            writer.writeInt(100);
            writer.flush();
            writer.writeUTF("diocane");
            writer.flush();
            while (true);

        }catch (Exception ex) {
            ex.printStackTrace();
        }


    }
}
