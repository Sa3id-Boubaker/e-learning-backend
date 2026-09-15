package projectelearning.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignupResult {
    private final String verificationToken;
    private final UserResponse user;
}