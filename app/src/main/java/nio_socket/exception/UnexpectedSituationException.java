package nio_socket.exception;

public class UnexpectedSituationException extends RuntimeException {

    public UnexpectedSituationException(final String message) {
        super(message);
    }

    public UnexpectedSituationException(final String message, final Throwable e) {
        super(message, e);
    }

    public UnexpectedSituationException(final Throwable e) {
        super(e);
    }
}
