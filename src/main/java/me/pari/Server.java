package me.pari;

import me.pari.controllers.Controller;
import me.pari.controllers.Ping;
import me.pari.controllers.auth.SignIn;
import me.pari.controllers.auth.SignOut;
import me.pari.controllers.auth.SignUp;
import me.pari.controllers.chats.JoinChat;
import me.pari.controllers.chats.LeaveChat;
import me.pari.controllers.chats.RemoveUser;
import me.pari.controllers.messages.GetMessages;
import me.pari.controllers.messages.SendMessage;
import me.pari.controllers.users.GetUser;
import me.pari.controllers.users.GetUsers;
import me.pari.types.Status;
import me.pari.types.tcp.Request;
import org.hydev.logger.HyLogger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;


public class Server extends Thread {

    // Logger
    private static final HyLogger LOGGER = new HyLogger("Server");

    // Server settings
    public static final int PORT = 7777;
    public static final int MAX_CLIENTS = 200;
    public static final String DATABASE_NAME = "database.db";

    // Singleton instance
    private static Server INSTANCE;

    public static HashMap<String, Class<? extends Controller>> controllers = new HashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private ServerSocket server;

    private Server() {

        if (INSTANCE != null)
            return;

        // Auth controllers
        controllers.put("SignUp".toLowerCase(), SignUp.class);
        controllers.put("SignIn".toLowerCase(), SignIn.class);
        controllers.put("SignOut".toLowerCase(), SignOut.class);

        // Chats controllers
        controllers.put("JoinChat".toLowerCase(), JoinChat.class);
        controllers.put("LeaveChat".toLowerCase(), LeaveChat.class);
        controllers.put("RemoveUser".toLowerCase(), RemoveUser.class);

        // Message controllers
        controllers.put("GetMessages".toLowerCase(), GetMessages.class);
        controllers.put("SendMessage".toLowerCase(), SendMessage.class);

        // Users controllers
        controllers.put("GetUsers".toLowerCase(), GetUsers.class);
        controllers.put("GetUser".toLowerCase(), GetUser.class);

        // Utils controllers
        controllers.put("Ping".toLowerCase(), Ping.class);
    }

    public static Server getInstance() {
        if (INSTANCE == null)
            INSTANCE = new Server();
        return INSTANCE;
    }

    @Override
    public void run() {
        try (ServerSocket s = new ServerSocket()) {

            // Bind the server
            s.bind(new InetSocketAddress(PORT));
            this.server = s;

            // Accept new connections
            while (isRunning.get()) {
                try {

                    // Max clients reached (improve performance)
                    if (Client.getClients().size() >= MAX_CLIENTS) {
                        Thread.onSpinWait();
                        continue;
                    }

                    // Wrap connections into Thread Clients
                    Socket clientSocket = s.accept();

                    // New connection
                    LOGGER.log("Client connected: " + clientSocket.getRemoteSocketAddress());

                    // Handle new connection
                    Client.startNew(clientSocket);

                } catch (IOException ex) {

                    // Exclude socket closed errors
                    if (!ex.getMessage().equalsIgnoreCase("socket closed")) {
                        LOGGER.warning("Error handling new client: " + ex.getMessage());
                    }
                }
            }

        } catch (IOException ex) {
            LOGGER.error("Error during server running: " + ex.getMessage());
        }
    }

    public void handle(Request r) throws IOException {
        LOGGER.log("Handled: " + Client.json.toJson(r));

        // Get method
        String method = r.getMethod();

        // Packet method is empty
        if (method == null) {
            r.sendResponse(Status.BAD_REQUEST, "BadRequest");
            return;
        }

        // TODO: Manage flood

        // Get class method
        Controller t;
        try {
            t = controllers.get(method.toLowerCase()).getDeclaredConstructor().newInstance();
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            ex.printStackTrace();
            return;
        }

        // Need to be authorized and Client is not authorized
        if (t.isNeededAuth() && !r.getClient().user.isAuth()) {
            r.sendResponse(Status.UNAUTHORIZED, "Unauthorized");
            return;
        }

        // Execute the action and respond to the client
        r.sendResponse(t.execute(r));
    }

    public void stopServer() {
        isRunning.set(false);
        try {
            if (server != null)
                server.close();
        } catch (IOException ex) {
            LOGGER.error("Error closing the server: " + ex.getMessage());
        }
    }

    public boolean isServerRunning() {
        return isRunning.get();
    }

}
