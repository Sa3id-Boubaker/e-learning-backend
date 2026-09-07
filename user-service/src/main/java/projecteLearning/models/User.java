package projecteLearning.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String id;

    private String firstName;
    private String lastName;

    @Indexed(unique = true)
    private String email;

    private String password;
    private Role role;
    private String profileImage;
    private String profileImagePublicId;
    private String phone;
    private String bio;
    private Boolean enabled;
    private Boolean mustChangePassword;

    // Vérification d'email — on stocke le HASH du code, jamais le code en clair
    private String verificationCodeHash;
    private LocalDateTime verificationCodeExpiresAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}