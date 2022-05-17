package me.pari.types;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import me.pari.Server;
import org.hydev.logger.HyLogger;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client extends Thread {

    // Logger
    private static final HyLogger LOGGER = new HyLogger("Client");

    // Synchronized client list
    private static final List<Client> clients = Collections.synchronizedList(new ArrayList<>());

    // Current client is connected
    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    // Current client socket info
    private final Socket socket;
    private final BufferedOutputStream output;
    private final DataInputStream input;

    public Client(Socket socket) throws IOException {
        this.socket = socket;
        this.output = new BufferedOutputStream(socket.getOutputStream());
        this.input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        synchronized (this) {
            clients.add(this);
            isConnected.set(true);
        }
    }

    @Override
    public void run() {
        Server server = Server.getInstance();
        System.out.println("AAAAA");
         while (isConnected.get()) {
            try {
                // Read bytes from the client

                int x = input.readInt();
                System.out.println(x);
                String test = input.readUTF();
                System.out.println(test);
                //Packet p = new Gson().fromJson(new String(buffer), Packet.class);

                // Handle the packet by the server
                //System.out.println(p);

            } catch (IOException e) {
                close();
                LOGGER.log("Error reading bytes: " + e.getMessage());
            }
        }
    }

    public void sendData(Packet data) throws IOException {
        output.write(data.toString().getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    public synchronized void close() {
        try {
            socket.close();
        } catch (IOException ex) {
            LOGGER.log("Error closing client: " + ex.getMessage());
        } finally {
            isConnected.set(false);
            clients.remove(this);
        }
    }

    public static synchronized List<Client> getClients() {
        return clients;
    }

}
