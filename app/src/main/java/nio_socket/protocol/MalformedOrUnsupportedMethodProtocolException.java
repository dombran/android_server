/**************************************************
 * Android Web Server
 * Based on JavaLittleWebServer (2008)
 * <p/>
 * Copyright (c) Piotr Polak 2008-2016
 **************************************************/

package nio_socket.protocol;

/**
 * Method too long or unknown (not supported).
 */
public class MalformedOrUnsupportedMethodProtocolException extends ProtocolException {

    public MalformedOrUnsupportedMethodProtocolException(final String message) {
        super(message);
    }
}
