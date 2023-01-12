package nio_socket.request;

import static nio_socket.utils.Constants.SOCKET_READ_BUFFER_SIZE_BYTES;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nio_socket.exception.LengthRequiredException;
import nio_socket.exception.PayloadTooLargeProtocolException;
import nio_socket.protocol.Cookie;
import nio_socket.protocol.Headers;
import nio_socket.protocol.HttpServletRequest;
import nio_socket.protocol.MalformedOrUnsupportedMethodProtocolException;
import nio_socket.protocol.MalformedStatusLineException;
import nio_socket.protocol.ProtocolException;
import nio_socket.protocol.RequestStatus;
import nio_socket.protocol.Statistics;
import nio_socket.protocol.StatusLineTooLongProtocolException;
import nio_socket.protocol.UnsupportedProtocolException;
import nio_socket.protocol.UriTooLongProtocolException;
import nio_socket.protocol.parser.MalformedInputException;
import nio_socket.protocol.parser.Parser;
import nio_socket.protocol.parser.impl.CookieParser;
import nio_socket.protocol.parser.impl.HeadersParser;
import nio_socket.protocol.parser.impl.QueryStringParser;
import nio_socket.protocol.parser.impl.RequestStatusParser;

public class Http_RequestFactory {

    private static final String DEFAULT_SCHEME = "http";
    private static final char NEWLINE = '\n';
    private static final String HTTP_1_0 = "HTTP/1.0";
    private static final String HTTP_1_1 = "HTTP/1.1";

    private static final String[] RECOGNIZED_METHODS = {
            "OPTIONS",
            "GET",
            "HEAD",
            "POST",
            "PUT",
            "DELETE",
            "TRACE",
            "CONNECT"
    };
    private static final int METHOD_MAX_LENGTH;

    private static final String HEADERS_END_DELIMINATOR = "\n\r\n";
    private static final int MINUMUM_HEADER_LINE_LENGTH = 3;
    private static final int URI_MAX_LENGTH = 2048;
    private static final int POST_MAX_LENGTH = 50 * 1024 * 1024;
    private static final int STATUS_MAX_LENGTH = 8 + URI_MAX_LENGTH + 9; // CONNECT + space + URI + space + HTTP/1.0
    private final ByteBuffer readBuffer = ByteBuffer.allocate(SOCKET_READ_BUFFER_SIZE_BYTES);
    private static final List<String> RECOGNIZED_METHODS_LIST = Arrays.asList(RECOGNIZED_METHODS);


    static {
        int maxMethodLength = 0;
        for (String method : RECOGNIZED_METHODS) {
            if (method.length() > maxMethodLength) {
                maxMethodLength = method.length();
            }
        }
        METHOD_MAX_LENGTH = maxMethodLength;
    }

    private final Parser<Headers> headersParser;
    private final Parser<RequestStatus> statusParser;
    private final Parser<Map<String, String>> queryStringParser;
    private final Parser<Map<String, Cookie>> cookieParser;

    public Http_RequestFactory(){
        headersParser = new HeadersParser();
        statusParser = new RequestStatusParser();
        queryStringParser = new QueryStringParser();
        cookieParser = new CookieParser();
    }

    public Http_RequestBuilder ParseRequest(SocketChannel channel) throws IOException, ProtocolException {
        ReadableByteChannel rb_channel = (ReadableByteChannel) channel;
        Socket clientInfo = channel.socket();
        Http_RequestBuilder.Builder builder = Http_RequestBuilder.createNewBuilder();

        RequestStatus status;
        try {
            status = statusParser.parse(getStatusLine(rb_channel));
        } catch (MalformedInputException e) {
            throw new MalformedStatusLineException("Malformed status line " + e.getMessage());
        }

        int uriLengthExceededWith = status.getUri().length() - URI_MAX_LENGTH;
        if (uriLengthExceededWith > 0) {
            throw new UriTooLongProtocolException("Uri length exceeded max length with"
                    + uriLengthExceededWith + " characters");
        }

        if (!isValidProtocol(status.getProtocol())) {
            throw new UnsupportedProtocolException("Protocol " + status.getProtocol() + " is not supported");
        }

        builder.withSecure(false)
                .withScheme(DEFAULT_SCHEME)
                .withRemoteAddr( clientInfo.getInetAddress().getHostAddress() )
                .withRemotePort( ((InetSocketAddress) clientInfo.getRemoteSocketAddress()).getPort() )
                .withRemoteHost( ((InetSocketAddress) clientInfo.getRemoteSocketAddress()).getHostName() )
                .withLocalAddr( clientInfo.getLocalAddress().getHostAddress() )
                .withLocalPort( clientInfo.getLocalPort()).withServerPort(clientInfo.getLocalPort() )
                .withLocalName( clientInfo.getLocalAddress().getHostName() )
                .withServerName( clientInfo.getInetAddress().getHostName() );

        try {
            builder.withGetParameters(queryStringParser.parse(status.getQueryString()));
        } catch (MalformedInputException e) {
            // This should never happen
        }

        Headers headers = computeHeaders(rb_channel);

        //builder.withServletContext(getImplicitServletContext())
        //.withInputStream(in)
        builder.withStatus(status)
                .withPathTranslated(status.getUri()) // TODO There is no way to make it work under Android
                .withPathInfo("")
                .withRemoteUser(null)
                .withPrincipal(null)
                .withHeaders(headers)
                .withCookies(getCookies(headers));

        if (status.getMethod().equalsIgnoreCase(HttpServletRequest.METHOD_POST)) {
            try {
                handlePostRequest(builder, rb_channel, headers);
            } catch (MalformedInputException e) {
                throw new ProtocolException("Malformed post input");
            }
        }

        return builder.build();

    }

    private Map<String, Cookie> getCookies(final Headers headers) {
        if (headers.containsHeader(Headers.HEADER_COOKIE)) {
            try {
                return cookieParser.parse(headers.getHeader(Headers.HEADER_COOKIE));
            } catch (MalformedInputException e) {
                // Returns an empty map
            }
        }
        return new HashMap<>();
    }

    private String getStatusLine(ReadableByteChannel channel)
            throws IOException, StatusLineTooLongProtocolException, MalformedOrUnsupportedMethodProtocolException {
        StringBuilder statusLine = new StringBuilder();
        byte[] buffer = new byte[1];
        int length = 0;
        boolean wasMethodRead = false;

        readBuffer.clear();
        int i_num = channel.read(readBuffer);
        readBuffer.flip();
        while (i_num > 0) {
            readBuffer.get(buffer);//.get(buffer, 0, buffer.length);
            i_num -= buffer.length;

            ++length;

            if (buffer[0] == NEWLINE) {
                break;
            }
            statusLine.append((char) buffer[0]);

            if (!wasMethodRead) {
                if (buffer[0] == ' ') {
                    wasMethodRead = true;
                    String method = statusLine.substring(0, statusLine.length() - 1).toUpperCase();
                    if (!RECOGNIZED_METHODS_LIST.contains(method)) {
                        Statistics.addBytesReceived(length);
                        throw new MalformedOrUnsupportedMethodProtocolException("Method " + method + " is not supported");
                    }
                } else {
                    if (length > METHOD_MAX_LENGTH) {
                        Statistics.addBytesReceived(length);
                        throw new MalformedOrUnsupportedMethodProtocolException("Method name is longer than expected");
                    }
                }
            }

            if (length > STATUS_MAX_LENGTH) {
                Statistics.addBytesReceived(length);
                throw new StatusLineTooLongProtocolException("Exceeded max size of " + STATUS_MAX_LENGTH);
            }
        }
        Statistics.addBytesReceived(length);

        return statusLine.toString();
    }

    private boolean isValidProtocol(final String protocol) {
        return protocol.equalsIgnoreCase(HTTP_1_0) || protocol.equalsIgnoreCase(HTTP_1_1);
    }

    private Headers computeHeaders(ReadableByteChannel channel) throws IOException {

        String headersString = getHeadersString(channel);
        if (headersString.length() > MINUMUM_HEADER_LINE_LENGTH) {
            try {
                return headersParser.parse(headersString);
            } catch (MalformedInputException e) {
                throw new ProtocolException("Malformed request headers");
            }
        }

        // TODO Write a test that sends a request containing status line only
        return new Headers();
    }
    private String getHeadersString(ReadableByteChannel channel) throws IOException {
        StringBuilder headersString = new StringBuilder();
        byte[] buffer;
        buffer = new byte[1];
        int headersEndSymbolLength = HEADERS_END_DELIMINATOR.length();

        readBuffer.clear();
        int i_num = channel.read(readBuffer);
        readBuffer.flip();

        while (i_num > 0) {
                readBuffer.get(buffer, 0, buffer.length);
                i_num -= buffer.length;

            headersString.append((char) buffer[0]);
            if (headersString.length() > headersEndSymbolLength) {
                String endChars = getEndChars(headersString, headersEndSymbolLength);
                if (endChars.equals(HEADERS_END_DELIMINATOR)) {
                    headersString.setLength(headersString.length() - headersEndSymbolLength);
                    break;
                }
            }
        }

        Statistics.addBytesReceived(headersString.length() + headersEndSymbolLength);
        return headersString.toString();
    }

    private String getEndChars(final StringBuilder headersString, final int headersEndSymbolLength) {
        return headersString.substring(headersString.length() - headersEndSymbolLength, headersString.length());
    }

    private void handlePostRequest(final Http_RequestBuilder.Builder builder,
                                   ReadableByteChannel channel,
                                   final Headers headers)
            throws IOException, MalformedInputException {
        int postLength;
        if (headers.containsHeader(Headers.HEADER_CONTENT_LENGTH)) {
            try {
                postLength = Integer.parseInt(headers.getHeader(Headers.HEADER_CONTENT_LENGTH));
            } catch (NumberFormatException e) {
                throw new MalformedInputException(e.getMessage());
            }
        } else {
            throw new LengthRequiredException();
        }

        // Only if post length is greater than 0
        // Keep 0 value - makes no sense to parse the data
        if (postLength < 1) {
            return;
        }

        if (postLength > POST_MAX_LENGTH) {
            throw new PayloadTooLargeProtocolException("Payload of " + postLength + "b exceeds the limit of "
                    + POST_MAX_LENGTH + "b");
        }

        //if (isMultipartRequest(headers)) {
        //    handlePostMultipartRequest(builder, headers, channel, postLength);
        //} else {
            handlePostPlainRequest(builder, channel, postLength);
        //}
    }

    //private boolean isMultipartRequest(final Headers headers) {
    //    return headers.containsHeader(Headers.HEADER_CONTENT_TYPE)
    //            && headers.getHeader(Headers.HEADER_CONTENT_TYPE).toLowerCase()
    //            .startsWith(MULTIPART_FORM_DATA_HEADER_START);
    //}

    private void handlePostPlainRequest(final Http_RequestBuilder.Builder builder,
                                        ReadableByteChannel channel,
                                        final int postLength)
            throws IOException, MalformedInputException {
        byte[] buffer;
        buffer = new byte[1];
        StringBuilder postLine = new StringBuilder();


        readBuffer.clear();
        int i_num = channel.read(readBuffer);
        readBuffer.flip();

        while (i_num > 0) {
            readBuffer.get(buffer, 0, buffer.length);
            i_num -= buffer.length;

            postLine.append((char) buffer[0]);
            if (postLine.length() == postLength) {
                break;
            }
        }
        Statistics.addBytesReceived(postLine.length());
        builder.withPostParameters(queryStringParser.parse(postLine.toString()));
    }

/*    private void handlePostMultipartRequest(final Http_RequestBuilder.Builder builder,
                                            final Headers headers,
                                            final ReadableByteChannel channel,
                                            final int postLength)
            throws IOException, MalformedInputException {

        String boundary = headers.getHeader(Headers.HEADER_CONTENT_TYPE);
        int boundaryPosition = boundary.toLowerCase().indexOf(BOUNDARY_START);
        builder.withMultipart(true);
        if (boundaryPosition > -1) {
            int boundaryStartPos = boundaryPosition + BOUNDARY_START.length();
            if (boundaryStartPos < boundary.length()) {
                boundary = boundary.substring(boundaryStartPos, boundary.length());
                MultipartRequestHandler mrh =
                        new MultipartRequestHandler(multipartHeadersPartParser, channel, postLength, boundary,
                                tempPath, MULTIPART_BUFFER_LENGTH);
                mrh.handle();

                builder.withPostParameters(mrh.getPost()).withUploadedFiles(mrh.getUploadedFiles());
            }
        }
    }*/

}
