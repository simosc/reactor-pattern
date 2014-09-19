package com.af.reactor.util;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;

public class NoneBlockingClient {
    
    static final Logger 
    
    final CloseableHttpAsyncClient httpclient;
    
    public NoneBlockingClient() {
        httpclient = HttpAsyncClients.createDefault();
        httpclient.start();
    }
    
    public void close() throws IOException {
        httpclient.close();
    }
    
    public void get(String url, final Callback callback) throws InterruptedException {
        
        final CountDownLatch latch = new CountDownLatch(1);
        final HttpGet request = new HttpGet("http://www.apache.org/");
        httpclient.execute(request, new FutureCallback<HttpResponse>() {

            public void completed(final HttpResponse response) {
                try {
                    latch.countDown();
                    HttpEntity entity = response.getEntity();
                    callback.setContent(IOUtils.toString(entity.getContent()));
                } catch (Exception e) {

                }
            }

            public void failed(final Exception ex) {
                latch.countDown();
            }

            public void cancelled() {
                latch.countDown();
            }
        });
        latch.await();
    }

    public static void main(final String[] args) throws Exception {
        CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();
        try {
            // Start the client
            httpclient.start();

            // One most likely would want to use a callback for operation result
            final CountDownLatch latch = new CountDownLatch(1);
            final HttpGet request = new HttpGet("http://www.apache.org/");
            httpclient.execute(request, new FutureCallback<HttpResponse>() {

                public void completed(final HttpResponse response) {
                    latch.countDown();
                    System.out.println(request.getRequestLine() + "->" + response.getEntity().toString());
                }

                public void failed(final Exception ex) {
                    latch.countDown();
                    System.out.println(request.getRequestLine() + "->" + ex);
                }

                public void cancelled() {
                    latch.countDown();
                    System.out.println(request.getRequestLine() + " cancelled");
                }

            });
            latch.await();
        } finally {
            httpclient.close();
        }
    }
    
    
    public interface Callback {
        
        void setContent(String content);
    }
}
