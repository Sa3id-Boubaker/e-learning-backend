package projecteLearning.exception;

import lombok.Getter;

@Getter
public class AccountNotVerifiedException extends RuntimeException {

    private final String verificationToken;

    public AccountNotVerifiedException(String verificationToken, String message) {
        super(message);
        this.verificationToken = verificationToken;
    }
}