// exception/UserNotFoundException.java
package projecteLearning.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) { super(message); }
}