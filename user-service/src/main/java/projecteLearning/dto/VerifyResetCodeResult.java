package projecteLearning.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VerifyResetCodeResult {
    private final String sessionToken;
    private final MessageResponse message;
}