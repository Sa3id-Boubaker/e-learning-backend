package projectelearning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResetTokenResponse {
    private String resetToken;
    private String message;
}