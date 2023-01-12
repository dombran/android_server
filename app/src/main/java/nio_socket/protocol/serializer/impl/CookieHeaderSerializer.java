/**************************************************
 * Android Web Server
 * Based on JavaLittleWebServer (2008)
 * <p/>
 * Copyright (c) Piotr Polak 2008-2016
 **************************************************/
package nio_socket.protocol.serializer.impl;

import java.util.Date;

import nio_socket.protocol.serializer.Serializer;
import nio_socket.protocol.Cookie;
import nio_socket.utils.DateProvider;
import nio_socket.utils.DateUtilities;
import nio_socket.utils.StringUtilities;

/**
 * Serializes cookie to text representation.
 *
 * @author Piotr Polak piotr [at] polak [dot] ro
 * @since 201611
 */
public class CookieHeaderSerializer implements Serializer<Cookie> {

    private static final String SEPARATOR = "; ";
    private static final String EQUALS = "=";
    private static final long MILLISECONDS_IN_SECOND = 1000L;

    private final DateProvider dateProvider;

    /**
     * @param dateProvider
     */
    public CookieHeaderSerializer(final DateProvider dateProvider) {
        this.dateProvider = dateProvider;
    }

    /**
     * Returns serialized cookie header value.
     *
     * @param cookie
     * @return
     */
    @Override
    public String serialize(final Cookie cookie) {
        StringBuilder sb = new StringBuilder();
        sb.append(cookie.getName())
                .append(EQUALS)
                .append(StringUtilities.urlEncode(cookie.getValue()));

        if (cookie.getMaxAge() != -1) {
            sb.append(SEPARATOR)
                    .append("Expires")
                    .append(EQUALS)
                    .append(getExpires(cookie.getMaxAge()));
        }
        if (cookie.getPath() != null) {
            sb.append(SEPARATOR)
                    .append("Path")
                    .append(EQUALS)
                    .append(cookie.getPath());
        }
        if (cookie.getDomain() != null) {
            sb.append(SEPARATOR)
                    .append("Domain")
                    .append(EQUALS)
                    .append(cookie.getDomain());
        }
        if (cookie.getComment() != null) {
            sb.append(SEPARATOR)
                    .append("Comment")
                    .append(EQUALS)
                    .append(cookie.getComment());
        }
        if (cookie.isHttpOnly()) {
            sb.append(SEPARATOR)
                    .append("HttpOnly");
        }
        if (cookie.isSecure()) {
            sb.append(SEPARATOR)
                    .append("Secure");
        }

        return sb.toString();
    }

    private String getExpires(final long maxAge) {
        long maxAgeMs = maxAge * MILLISECONDS_IN_SECOND;
        return DateUtilities.dateFormat(new Date(dateProvider.now().getTime() + maxAgeMs));
    }
}
