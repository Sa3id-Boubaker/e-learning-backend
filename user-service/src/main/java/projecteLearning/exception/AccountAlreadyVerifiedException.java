// exception/AccountAlreadyVerifiedException.java
package projecteLearning.exception;

public class AccountAlreadyVerifiedException extends RuntimeException {
    public AccountAlreadyVerifiedException(String message) { super(message); }
}