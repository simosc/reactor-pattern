package com.af.reactor.nonblocking;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

/**
 * Accept new client connection
 */
public class Acceptor extends Handler {

	public static final Logger LOG = Logger.getLogger(Acceptor.class.getName());

	@Override
	public void accept(SelectionKey key) throws IOException {

		LOG.info("Accept client connection");
		
		// For an accept to be pending the channel must be a server socket
		// channel
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key
				.channel();

		// Accept the connection and make it non-blocking
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);

		// Register the new SocketChannel with our Selector, indicating
		// we'd like to be notified when there's data waiting to be read
		socketChannel.register(key.selector(), SelectionKey.OP_READ,
				new ForwardProxy());
	}
}
