package com.af.reactor.nonblocking;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

/**
 * Accept new client connection
 */
public class Acceptor extends Handler {
	
	public static final Logger LOG = Logger.getLogger(Acceptor.class.getName());
	
	private final ServerSocketChannel server;
	private final Selector selector;

	public Acceptor(Selector selector, ServerSocketChannel server) {
		this.server = server;
		this.selector = selector;
	}

	@Override
	public void accept(SelectionKey serverKey) throws IOException {

		SocketChannel channel = server.accept();
		new ForwardProxy(selector, channel);
	}
}
