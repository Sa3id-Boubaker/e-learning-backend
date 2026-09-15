// exception/UserNotFoundException.java
package projectelearning.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) { super(message); }
}