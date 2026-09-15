package projectelearning.exception;

public class ResetTokenExpiredException extends RuntimeException {
    public ResetTokenExpiredException(String message) { super(message); }
}