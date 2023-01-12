/**************************************************
 * Android Web Server
 * Based on JavaLittleWebServer (2008)
 * <p/>
 * Copyright (c) Piotr Polak 2008-2017
 **************************************************/

package nio_socket.protocol;

/**
 * Unsupported protocol.
 */
public class UnsupportedProtocolException extends ProtocolException {

    public UnsupportedProtocolException(final String message) {
        super(message);
    }
}
