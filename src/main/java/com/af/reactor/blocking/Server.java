package com.af.reactor.blocking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server implements Runnable {

    static final int PORT = 4711;
    static final Logger log = Logger.getLogger(Server.class.getName());
    
    static ExecutorService pool = Executors.newFixedThreadPool(2);

    public void run() {

        try (ServerSocket server = new ServerSocket(PORT)) {
            while (!Thread.interrupted()) {
                Socket socket = server.accept();
                pool.execute(new Handler(socket));
            }
        } catch (IOException ex) {
            log.log(Level.SEVERE,
                    "Failed to execute server thread: " + ex.getMessage(), ex);
        }
    }

    public static void main(String[] args) throws Exception {

        Server server = new Server();
        new Thread(server).start();
    }
}