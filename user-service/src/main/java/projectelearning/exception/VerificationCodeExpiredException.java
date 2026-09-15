// exception/VerificationCodeExpiredException.java
package projectelearning.exception;

public class VerificationCodeExpiredException extends RuntimeException {
    public VerificationCodeExpiredException(String message) { super(message); }
}