package projecteLearning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import projecteLearning.models.Role;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String bio;
    private String profileImage;
    private Role role;
    private Boolean enabled;
    private LocalDateTime createdAt;
}