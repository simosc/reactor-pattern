package com.af.reactor.blocking;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server implements Runnable {

    private static final int PORT = 4711;
    private static final Logger LOG = Logger.getLogger(Server.class.getName());
    
    static ExecutorService pool = Executors.newFixedThreadPool(2);

    public void run() {

        try (ServerSocket server = new ServerSocket(PORT)) {
        	// Wait for an connection
            while (!Thread.interrupted()) {
				LOG.info("Wait for connection");
                Socket socket = server.accept();
                LOG.info("Got a connection");
                pool.execute(new Handler(socket));
                LOG.info("Connection: DONE");
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE,
                    "Failed to execute server thread: " + ex.getMessage(), ex);
        }
    }

    public static void main(String[] args) throws Exception {

        Server server = new Server();
        new Thread(server).start();
    }
}