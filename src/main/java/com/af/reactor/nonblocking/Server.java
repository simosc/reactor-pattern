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

	private final Selector selector;
	private final ServerSocketChannel server;

	public Server(int port) throws Exception {

		selector = Selector.open();
		server = ServerSocketChannel.open();
		server.socket().bind(new InetSocketAddress(port));
		server.configureBlocking(false);
		server.register(selector, SelectionKey.OP_ACCEPT, new Acceptor(selector, server));
	}

	public void run() {

		LOG.info("Server listening to port: " + server.socket().getLocalPort());
		try {
			while (selector.select() > 0) {

				Set<SelectionKey> selected = selector.selectedKeys();
				for (SelectionKey key : selected) {

					Handler handler = (Handler) key.attachment();
					if (!key.isValid() || handler == null) {
						continue;
					} else if (key.isAcceptable()) {
						handler.accept(key);
					} else if (key.isReadable()) {
						handler.read();
					} else if (key.isWritable()) {
						handler.write();
					} else if (key.isConnectable()) {
						handler.connect();
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