package projecteLearning.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResendCodeResult {
    private final String verificationToken;
    private final MessageResponse message;
}