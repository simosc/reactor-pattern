package com.af.reactor.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.entity.EntityDeserializer;
import org.apache.http.impl.entity.EntitySerializer;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.DefaultHttpRequestParser;
import org.apache.http.impl.io.DefaultHttpResponseParser;
import org.apache.http.impl.io.DefaultHttpResponseWriter;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.impl.io.SessionOutputBufferImpl;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.HttpMessageWriter;
import org.apache.http.protocol.HTTP;

public class HttpUtil {

    private static final int BUFFER_SIZE = 8192;
    private static final String NEW_LINE = "\r\n";

    public static HttpRequest parseRequest(String content) throws IOException {

        return parseRequest(IOUtils.toInputStream(content));
    }

    public static HttpRequest parseRequest(SocketChannel channel)
            throws IOException {

        return parseRequest(toString(channel).toString());
    }

    public static HttpRequest parseRequest(InputStream in) throws IOException {

        try {
            HttpTransportMetricsImpl metrics = new HttpTransportMetricsImpl();
            SessionInputBufferImpl inbuffer = new SessionInputBufferImpl(
                    metrics, BUFFER_SIZE);
            inbuffer.bind(in);
            HttpMessageParser<HttpRequest> requestParser = new DefaultHttpRequestParser(
                    inbuffer);
            return requestParser.parse();
        } catch (HttpException e) {
            throw new IOException(e);
        }
    }

    public static Map<String, String> parseParams(HttpRequest request)
            throws IOException {

        try {
            Map<String, String> mapped = new HashMap<>();
            String uri = request.getRequestLine().getUri();
            List<NameValuePair> params = URLEncodedUtils.parse(new URI(uri),
                    "UTF-8");
            for (NameValuePair param : params) {
                mapped.put(param.getName(), param.getValue());
            }
            return mapped;
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    @SuppressWarnings("deprecation")
    private static HttpResponse parseResponse(StringBuilder content,
            boolean complete) throws IOException {

        try {
            SessionInputBufferImpl inbuffer = new SessionInputBufferImpl(
                    new HttpTransportMetricsImpl(), BUFFER_SIZE);
            HttpMessageParser<HttpResponse> responseParser = new DefaultHttpResponseParser(
                    inbuffer);
            inbuffer.bind(IOUtils.toInputStream(content));
            HttpResponse response = responseParser.parse();

            ContentLengthStrategy strategy = new StrictContentLengthStrategy(
                    ContentLengthStrategy.IDENTITY);
            Header transferEncoding = response
                    .getFirstHeader(HTTP.TRANSFER_ENCODING);
            boolean isChuncked = false;
            if (transferEncoding != null
                    && HTTP.CHUNK_CODING.equals(transferEncoding.getValue())) {
                strategy = new StrictContentLengthStrategy(
                        ContentLengthStrategy.CHUNKED);
                isChuncked = true;
            }

            HttpEntity entity = null;
            if (!isChuncked || complete) {
                EntityDeserializer entityDeserializer = new EntityDeserializer(
                        strategy);
                entity = entityDeserializer.deserialize(inbuffer, response);
            } else {
                StringHttpEntity chunkedEntity = new StringHttpEntity();
                chunkedEntity.setChunked(isChuncked);
                readEntity(chunkedEntity, content);
                entity = chunkedEntity;
            }
            response.setEntity(entity);
            return response;
        } catch (HttpException e) {
            throw new IOException(e);
        }
    }

    public static HttpResponse parseResponse(SocketChannel channel)
            throws IOException {

        return parseResponse(toString(channel), false);
    }

    public static boolean appendResponse(HttpResponse response,
            StringBuilder content) throws IOException {

        StringHttpEntity entity = (StringHttpEntity) response.getEntity();
        if (entity.isChunked() && readEntity(entity, content)) {
            response.setEntity(parseResponse(entity.getRawContent(), true)
                    .getEntity());
            return true;
        }
        return false;
    }

    public static boolean appendResponse(HttpResponse response,
            SocketChannel channel) throws IOException {

        return appendResponse(response, toString(channel));
    }

    public static void writeRequest(HttpRequest request, SocketChannel channel)
            throws IOException {

        StringBuilder buf = new StringBuilder();
        buf.append(request.getRequestLine());
        buf.append(NEW_LINE);
        writeHeader(request, buf);
        ByteBuffer output = ByteBuffer.wrap(buf.toString().getBytes());
        channel.write(output);
    }

    public static void writeResponse(HttpResponse response,
            SocketChannel channel) throws IOException {

        StringBuilder buf = new StringBuilder();
        buf.append(response.getStatusLine());
        buf.append(NEW_LINE);
        writeHeader(response, buf);
        buf.append(IOUtils.toString(response.getEntity().getContent()));
        ByteBuffer output = ByteBuffer.wrap(buf.toString().getBytes());
        channel.write(output);
    }

    @SuppressWarnings("deprecation")
    public static void writeResponse(HttpResponse response, OutputStream out)
            throws IOException {

        try {
            SessionOutputBufferImpl outbuffer = new SessionOutputBufferImpl(
                    new HttpTransportMetricsImpl(), BUFFER_SIZE);
            outbuffer.bind(out);
            HttpMessageWriter<HttpResponse> responseWriter = new DefaultHttpResponseWriter(
                    outbuffer);
            responseWriter.write(response);
            EntitySerializer entitySerializer = new EntitySerializer(
                    StrictContentLengthStrategy.INSTANCE);
            entitySerializer.serialize(outbuffer, response,
                    response.getEntity());
        } catch (HttpException e) {
            throw new IOException(e);
        }
    }

    private static boolean readEntity(StringHttpEntity entity,
            StringBuilder content) {

        if (content.length() == 0) {
            return true;
        }
        entity.append(content);
        return !entity.isChunked();
    }

    private static void writeHeader(HttpMessage message, StringBuilder buf) {

        for (Header header : message.getAllHeaders()) {
            buf.append(header.getName());
            buf.append(": ");
            buf.append(header.getValue());
            buf.append(NEW_LINE);
        }
        buf.append(NEW_LINE);
    }

    public static StringBuilder toString(SocketChannel channel)
            throws IOException {

        StringBuilder builder = new StringBuilder();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        while ((channel.read(buffer)) > 0) {
            buffer.flip();
            byte[] bytes = new byte[buffer.limit()];
            buffer.get(bytes);
            builder.append(new String(bytes));
            buffer.clear();
        }
        return builder;
    }

    public static class StringHttpEntity extends AbstractHttpEntity implements
            Cloneable {

        private long chunckSize;

        protected StringBuilder content;

        public StringHttpEntity(final ContentType contentType)
                throws UnsupportedCharsetException {
            super();

            Charset charset = contentType != null ? contentType.getCharset()
                    : null;
            if (charset == null) {
                charset = HTTP.DEF_CONTENT_CHARSET;
            }
            this.content = new StringBuilder();
            if (contentType != null) {
                setContentType(contentType.toString());
            }
        }

        public StringHttpEntity() throws UnsupportedEncodingException {
            this(ContentType.DEFAULT_TEXT);
        }

        public boolean isRepeatable() {
            return true;
        }

        public long getContentLength() {
            return this.content.length();
        }

        public InputStream getContent() throws IOException {
            return IOUtils.toInputStream(content.toString());
        }

        public StringBuilder getRawContent() {
            return content;
        }

        public void setRawContent(StringBuilder content) {
            this.content = content;
        }

        public StringBuilder append(StringBuilder string) {
            return content.append(string);
        }

        public void writeTo(final OutputStream outstream) throws IOException {
            outstream.write(this.content.toString().getBytes());
            outstream.flush();
        }

        public boolean isStreaming() {
            return false;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        public boolean isChunkRead() {
            return content.length() <= chunckSize;
        }

        public long getChuckSize() {
            return chunckSize;
        }

        public void setChuckSize(long chuckSize) {
            this.chunckSize = chuckSize;
        }
    }
}
