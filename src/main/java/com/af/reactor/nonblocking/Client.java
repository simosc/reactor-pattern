package com.af.reactor.nonblocking;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
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
		LOG.info("Execute request: " + url);
		final HttpGet request = new HttpGet(url);
		httpclient.execute(request, this);
		LOG.info("Execute request: DONE");
	}

	public void close() {
		try {
			LOG.info("Close client connection");
			httpclient.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void completed(HttpResponse result) {
		try {
			LOG.info("Set content from request");
			receiver.setContent(IOUtils.toString(result.getEntity().getContent()));
			LOG.info("Set content from request: DONE");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void failed(Exception ex) {
		LOG.severe("Failed to execute request: " + ex.getMessage());
		receiver.setContent(ex.getMessage());
	}

	@Override
	public void cancelled() {
		LOG.severe("Cancelled request");
		receiver.setContent("cancelled");
	}

	/**
	 * Functional interface for content callback
	 */
	@FunctionalInterface
	public interface Receiver {

		/**
		 * Set the content
		 * 
		 * @param data
		 *            the content data
		 */
		public abstract void setContent(String data);
	}
}
