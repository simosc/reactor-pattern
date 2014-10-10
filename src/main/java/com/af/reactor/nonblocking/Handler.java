package com.af.reactor.nonblocking;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Handler base class
 */
public class Handler {

	public void read(SelectionKey key) throws IOException {

	}

	public void write(SelectionKey key) throws IOException {

	}

	public void accept(SelectionKey selectionKey) throws IOException {

	}

	public void connect(SelectionKey key) throws IOException {

	}
}
