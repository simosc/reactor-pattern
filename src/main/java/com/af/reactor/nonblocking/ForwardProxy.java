package com.af.reactor.nonblocking;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

import com.af.reactor.util.HttpUtil;

/**
 * Forward Proxy
 */
public class ForwardProxy extends Handler {

	public static final Logger LOG = Logger.getLogger(ForwardProxy.class
			.getName());

	private String content = null;
	private Client client = null;

	@Override
	public void read(SelectionKey key) throws IOException {

		LOG.info("Read client connection");

		// Create a client to proxy the request with. Use function
		client = new Client((x) -> this.setContent(x));
		String url = "http://localhost:9234";
		client.execute(url);

		// Indicate that we'd like to write data back to source
		key.interestOps(SelectionKey.OP_WRITE);

		LOG.info("Read client connection: DONE");
	}

	public void setContent(String content) {

		this.content = content;
		this.client.close();
	}

	@Override
	public void write(SelectionKey key) throws IOException {

		SocketChannel channel = (SocketChannel) key.channel();
		if (this.content != null) {
			LOG.info("Write data back to source");
			HttpUtil.writeResponse(this.content, channel);
			channel.close();
			LOG.info("Write data back to source: DONE");
		}
	}
}
