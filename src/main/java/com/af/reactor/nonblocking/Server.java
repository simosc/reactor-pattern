package com.af.reactor.nonblocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server implements Runnable {

	public static final Logger LOG = Logger.getLogger(Server.class.getName());

	// The selector we'll be monitoring
	private final Selector selector;

	// The channel on which we'll accept connections
	private final ServerSocketChannel server;

	public Server(int port) throws Exception {

		// Create a new selector
		selector = Selector.open();

		// Create a new non-blocking server socket channel
		server = ServerSocketChannel.open();
		server.configureBlocking(false);

		// Bind the server socket to the specified address and port
		server.socket().bind(new InetSocketAddress(port));

		// Register the server socket channel, indicating an interest in
		// accepting new connections. Attach a "handler" to channel, the
		// attachment is accessible through the "selection key" when it
		// "interest" gets triggered
		server.register(selector, SelectionKey.OP_ACCEPT, new Acceptor());
	}

	public void run() {

		LOG.info("Server listening to port: " + server.socket().getLocalPort());
		try {

			// Wait for an event from one of the registered channels
			while (selector.select() > 0) {

				LOG.info("Got event from a channel");
				
				// Iterate over the set of keys for which events are available
				Set<SelectionKey> selected = selector.selectedKeys();
				for (SelectionKey key : selected) {

					// Get the attached handler
					Handler handler = (Handler) key.attachment();

					// Check what event is available and deal with it
					if (!key.isValid() || handler == null) {
						continue;
					} else if (key.isAcceptable()) {
						// Tests whether this key's channel is ready to accept a
						// new socket connection
						handler.accept(key);
					} else if (key.isReadable()) {
						// Tests whether this key's channel is ready to accept a
						// new socket connection
						handler.read(key);
					} else if (key.isWritable()) {
						// Tests whether this key's channel is ready to accept a
						// new socket connection
						handler.write(key);
					} else if (key.isConnectable()) {
						// Tests whether this key's channel has either finished,
						// or failed to finish, its socket-connection operation.
						handler.connect(key);
					}
				}
				selected.clear();
			}
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Failed to handle selector", ex);
		}
	}

	public static void main(String[] args) throws Exception {

		Server server = new Server(4711);
		server.run();
	}
}