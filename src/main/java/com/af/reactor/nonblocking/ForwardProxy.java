package com.af.reactor.nonblocking;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;

import org.apache.http.HttpRequest;

import com.af.reactor.util.HttpUtil;

/**
 * Forward Proxy
 */
public class ForwardProxy extends Handler {

	private final SocketChannel channel;
	private final SelectionKey key;

	private String content = null;
	private Client client = null;

	public ForwardProxy(Selector selector, SocketChannel channel)
			throws IOException {

		this.channel = channel;
		channel.configureBlocking(false);
		key = channel.register(selector, SelectionKey.OP_READ, this);
		selector.wakeup();
	}

	@Override
	public void read() throws IOException {

		HttpRequest request = HttpUtil.parseRequest(channel);
		Map<String, String> params = HttpUtil.parseParams(request);
		client = new Client((x) -> this.setContent(x));
		String url = "http://pam.wikipedia.org/w/index.php?search="
				+ params.get("q");
		client.execute(url);
		key.interestOps(SelectionKey.OP_WRITE);
	}

	public void setContent(String content) {
		
		this.content = content;
		this.client.close();
	}

	@Override
	public void write() throws IOException {

		if (this.content != null) {
			HttpUtil.writeResponse(this.content, channel);
			channel.close();
		}
	}
}
