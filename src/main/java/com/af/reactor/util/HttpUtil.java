package com.af.reactor.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.entity.EntitySerializer;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.DefaultHttpRequestParser;
import org.apache.http.impl.io.DefaultHttpResponseWriter;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.impl.io.SessionOutputBufferImpl;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.HttpMessageWriter;
import org.apache.http.message.BasicHttpResponse;
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

    public static void writeResponse(String content,
            SocketChannel channel) throws IOException {

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

		HttpUtil.writeResponse(response, channel);
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
}
