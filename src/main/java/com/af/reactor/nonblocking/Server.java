package com.af.reactor.nonblocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HTTP;

import com.af.reactor.util.HttpUtil;

public class Server implements Runnable {

    public static final Logger log = Logger.getLogger(Server.class.getName());

    private final Selector selector;
    private final ServerSocketChannel server;

    public Server(int port) throws Exception {

        selector = Selector.open();
        server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(port));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT, new Acceptor());
    }

    public void run() {

        log.info("Server listening to port: " + server.socket().getLocalPort());
        try {
            while (selector.select() > 0) {

                Set<SelectionKey> selected = selector.selectedKeys();
                for (SelectionKey key : selected) {

                    Handler handler = (Handler) key.attachment();
                    if (!key.isValid() || handler == null) {
                        continue;
                    } else if (key.isAcceptable()) {
                        handler.accept(key);
                    } else if (key.isReadable()) {
                        handler.read();
                    } else if (key.isWritable()) {
                        handler.write();
                    } else if (key.isConnectable()) {
                        handler.connect();
                    }
                }
                selected.clear();
            }
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Failed to handle selector", ex);
        }
    }

    public static void main(String[] args) throws Exception {

        Server server = new Server(4711);
        server.run();
    }

    /**
     * Accept new client connection
     */
    private class Acceptor extends Handler {

        @Override
        public void accept(SelectionKey serverKey) throws IOException {

            SocketChannel channel = server.accept();
            new ForwardProxy(selector, channel);
        }
    }

    /**
     * Forward proxy
     */
    private class ForwardProxy extends Handler {

        private String content = null;
        private SocketChannel channel;
        private SelectionKey key;

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
            new Client(this, selector, "www.dn.se", 80,
                    "/"); // + params.get("q"));
            key.interestOps(SelectionKey.OP_WRITE);
        }

        public void setContent(String content) {
            this.content = content;
        }

        @Override
        public void write() throws IOException {

            if (this.content != null) {

                String timestamp = DateFormatUtils.SMTP_DATETIME_FORMAT
                        .format(new Date());
                ProtocolVersion version = new ProtocolVersion("HTTP", 1, 1);
                BasicHttpResponse response = new BasicHttpResponse(version,
                        200, "OK");
                response.addHeader(HTTP.DATE_HEADER, timestamp);
                response.addHeader(HTTP.SERVER_HEADER, "java-lab-2/0.0.1");
                response.addHeader(HTTP.CONTENT_TYPE,
                        "text/html; charset=utf-8");
                response.addHeader(HTTP.CONTENT_LEN,
                        String.valueOf(content.length()));
                BasicHttpEntity entity = new BasicHttpEntity();
                entity.setContent(IOUtils.toInputStream(this.content));
                response.setEntity(entity);

                HttpUtil.writeResponse(response, channel);
                channel.close();
            }
        }
    }
    
    private class Client2 extends Handler {
    	
    	public Client2() {
			// TODO Auto-generated constructor stub
		}
    }

    private class Client extends Handler {

        final private SocketChannel channel;
        final private ForwardProxy receiver;
        final private SelectionKey key;
        final private String uri;
        final private String url;
        HttpResponse response;

        public Client(ForwardProxy receiver, Selector selector, String url,
                int port, String uri) throws IOException {

            this.receiver = receiver;
            this.url = url;
            this.uri = uri;
            this.channel = SocketChannel.open();
            this.channel.configureBlocking(false);
            this.channel.connect(new InetSocketAddress(url, port));
            this.key = channel
                    .register(selector, SelectionKey.OP_CONNECT, this);
        }

        @Override
        public void read() throws IOException {

            if (response == null) {
                response = HttpUtil.parseResponse(channel);
                if (!response.getEntity().isChunked()) {
                    receiver.setContent(IOUtils.toString(response.getEntity()
                            .getContent()));
                    channel.close();
                }                
            } else if (HttpUtil.appendResponse(response, channel)) {
                receiver.setContent(IOUtils.toString(response.getEntity()
                        .getContent()));
                channel.close(); 
            }
        }

        @Override
        public void write() throws IOException {

            BasicHttpRequest request = new BasicHttpRequest("GET", uri);
            request.addHeader(HTTP.USER_AGENT, "java-lab-2");
            // request.addHeader("Accept", "text/*");
            request.addHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
            request.addHeader(HTTP.TARGET_HOST, url);
            HttpUtil.writeRequest(request, channel);
            key.interestOps(SelectionKey.OP_READ);
        }

        @Override
        public void connect() throws IOException {

            if (channel.isConnectionPending()) {
                channel.finishConnect();
                key.interestOps(SelectionKey.OP_WRITE);
            }
        }
    }
}