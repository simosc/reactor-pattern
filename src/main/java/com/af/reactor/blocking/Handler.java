package com.af.reactor.blocking;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.af.reactor.util.HttpUtil;

public class Handler implements Runnable {

	private static final Logger LOG = Logger.getLogger(Handler.class.getName());

	private final Socket socket;

	public Handler(Socket socket) {
		this.socket = socket;
	}

	public void run() {

		try (InputStream in = socket.getInputStream();
				OutputStream out = socket.getOutputStream()) {

		    // Read request
		    HttpUtil.parseRequest(in);
		    
			// Read from proxy
			String content = doGet("http://localhost:9234");

			// Write back response
			HttpUtil.writeResponse(content, out);

			// Close connection
			socket.close();

		} catch (IOException ex) {
			LOG.log(Level.SEVERE,
					"Failed to execute handler: " + ex.getMessage(), ex);
		}
	}

	private String doGet(String url) throws IOException {

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			HttpGet httpget = new HttpGet(url);
			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

				public String handleResponse(final HttpResponse response)
						throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if (status >= 200 && status < 300) {
						HttpEntity entity = response.getEntity();
						return entity != null ? EntityUtils.toString(entity)
								: null;
					} else {
						throw new ClientProtocolException(
								"Unexpected response status: " + status);
					}
				}

			};
			return httpclient.execute(httpget, responseHandler);
		}
	}
}