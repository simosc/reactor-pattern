package com.af.reactor.nonblocking;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

public class Client implements FutureCallback<HttpResponse> {
	
	public static final Logger LOG = Logger.getLogger(Client.class.getName());

	private final Receiver receiver;
	private final CloseableHttpAsyncClient httpclient;

	public Client(Receiver receiver) throws IOException {
		this.receiver = receiver;
		httpclient = HttpAsyncClients.createDefault();
		httpclient.start();
	}

	public void execute(String url) {
		final HttpGet request = new HttpGet(url);
		httpclient.execute(request, this);
	}

	public void close() {
		try {
			httpclient.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void completed(HttpResponse result) {
		try {
			HttpEntity entity = result.getEntity();
			receiver.setContent(IOUtils.toString(entity.getContent()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void failed(Exception ex) {
		receiver.setContent(ex.getMessage());
	}

	@Override
	public void cancelled() {
		receiver.setContent("cancelled");
	}

	@FunctionalInterface
	public interface Receiver {
		public abstract void setContent(String data);
	}
}
