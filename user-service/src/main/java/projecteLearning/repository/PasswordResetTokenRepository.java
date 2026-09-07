package projecteLearning.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import projecteLearning.models.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByEmail(String email);

    Optional<PasswordResetToken> findByResetTokenHash(String resetTokenHash);
}