// exception/VerificationCodeExpiredException.java
package projecteLearning.exception;

public class VerificationCodeExpiredException extends RuntimeException {
    public VerificationCodeExpiredException(String message) { super(message); }
}