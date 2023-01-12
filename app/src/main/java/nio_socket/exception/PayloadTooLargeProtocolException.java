/**************************************************
 * Android Web Server
 * Based on JavaLittleWebServer (2008)
 * <p/>
 * Copyright (c) Piotr Polak 2008-2017
 **************************************************/

package nio_socket.exception;

import nio_socket.protocol.ProtocolException;

/**
 * Payload too large.
 */
public class PayloadTooLargeProtocolException extends ProtocolException {

    public PayloadTooLargeProtocolException(final String message) {
        super(message);
    }
}
