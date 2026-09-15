package projectelearning.exception;

public class ResetCodeExpiredException extends RuntimeException {
    public ResetCodeExpiredException(String message) { super(message); }
}