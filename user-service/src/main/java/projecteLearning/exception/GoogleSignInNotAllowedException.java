package projecteLearning.exception;

public class GoogleSignInNotAllowedException extends RuntimeException {
    public GoogleSignInNotAllowedException(String message) {
        super(message);
    }
}