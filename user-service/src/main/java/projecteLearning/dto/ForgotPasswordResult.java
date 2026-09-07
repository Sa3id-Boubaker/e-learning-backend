package projecteLearning.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ForgotPasswordResult {
    private final String requestToken; // null si l'email n'existe pas
    private final MessageResponse message;
}