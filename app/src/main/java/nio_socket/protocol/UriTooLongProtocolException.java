/**************************************************
 * Android Web Server
 * Based on JavaLittleWebServer (2008)
 * <p/>
 * Copyright (c) Piotr Polak 2008-2016
 **************************************************/

package nio_socket.protocol;

/**
 * URI exceeds URI limit.
 */
public class UriTooLongProtocolException extends ProtocolException {

    public UriTooLongProtocolException(final String message) {
        super(message);
    }
}
