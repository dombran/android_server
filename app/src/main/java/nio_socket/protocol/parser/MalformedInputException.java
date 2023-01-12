/**************************************************
 * Android Web Server
 * Based on JavaLittleWebServer (2008)
 * <p/>
 * Copyright (c) Piotr Polak 2016-2017
 **************************************************/

package nio_socket.protocol.parser;

/**
 * Malformed input exception.
 */
public class MalformedInputException extends Exception {

    public MalformedInputException(final String s) {
        super(s);
    }
}
