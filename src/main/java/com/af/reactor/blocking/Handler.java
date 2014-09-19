package com.af.reactor.blocking;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import com.af.reactor.util.HttpUtil;

public class Handler implements Runnable {

    final Socket socket;

    public static final Logger log = Logger.getLogger(Handler.class.getName());

    public Handler(Socket socket) {
        this.socket = socket;
    }

    public void run() {

        try (InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream()) {

            // Read request
            HttpRequest request = HttpUtil.parseRequest(in);
            Map<String, String> params = HttpUtil.parseParams(request);

            // Read from source
            String content = executeClient("http://pam.wikipedia.org/w/index.php?search="
                    + params.get("q"));

            // Write back response
            String timestamp = DateFormatUtils.SMTP_DATETIME_FORMAT
                    .format(new Date());
            ProtocolVersion version = new ProtocolVersion("HTTP", 1, 1);
            BasicHttpResponse response = new BasicHttpResponse(version, 200,
                    "OK");
            response.addHeader(HTTP.DATE_HEADER, timestamp);
            response.addHeader(HTTP.SERVER_HEADER, "java-lab-2/0.0.1");
            response.addHeader(HTTP.CONTENT_TYPE, "text/html; charset=utf-8");
            response.addHeader(HTTP.CONTENT_LEN,
                    String.valueOf(content.length()));
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(IOUtils.toInputStream(content));
            response.setEntity(entity);

            HttpUtil.writeResponse(response, out);

        } catch (IOException ex) {
            log.log(Level.SEVERE,
                    "Failed to execute handler: " + ex.getMessage(), ex);
        }
    }

    private String executeClient(String url) throws IOException {

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