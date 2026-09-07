package projecteLearning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import projecteLearning.dto.*;
import projecteLearning.exception.*;
import projecteLearning.models.PasswordResetToken;
import projecteLearning.models.User;
import projecteLearning.repository.PasswordResetTokenRepository;
import projecteLearning.repository.UserRepository;
import projecteLearning.security.JwtUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtils jwtUtils;

    @Value("${password-reset.code.expiration-minutes:10}")
    private long codeExpirationMinutes;

    @Value("${password-reset.token.expiration-minutes:15}")
    private long resetTokenExpirationMinutes;

    public ForgotPasswordResult forgotPassword(ForgotPasswordRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("No account found with this email"));

        LocalDateTime now = LocalDateTime.now();
        String plainCode = generateVerificationCode();

        PasswordResetToken entry = passwordResetTokenRepository.findByEmail(user.getEmail())
                .orElse(PasswordResetToken.builder()
                        .email(user.getEmail())
                        .createdAt(now)
                        .build());

        entry.setCodeHash(passwordEncoder.encode(plainCode));
        entry.setCodeExpiresAt(now.plusMinutes(codeExpirationMinutes));
        entry.setCodeVerified(false);
        entry.setResetTokenHash(null);
        entry.setResetTokenExpiresAt(null);
        entry.setUpdatedAt(now);

        passwordResetTokenRepository.save(entry);

        emailService.sendPasswordResetCode(user.getEmail(), user.getFirstName(), plainCode, codeExpirationMinutes);

        String requestToken = jwtUtils.generatePasswordResetRequestToken(user.getEmail(), codeExpirationMinutes);

        return new ForgotPasswordResult(requestToken,
                new MessageResponse("A verification code has been sent to your email."));
    }


    public VerifyResetCodeResult verifyResetCode(String requestToken, VerifyResetCodeRequest request) {

        String email = resolveRequestEmail(requestToken);

        PasswordResetToken entry = passwordResetTokenRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidResetCodeException("Invalid or expired verification code"));

        if (entry.getCodeHash() == null || !passwordEncoder.matches(request.getCode(), entry.getCodeHash())) {
            throw new InvalidResetCodeException("Invalid or expired verification code");
        }

        if (entry.getCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResetCodeExpiredException("Verification code has expired. Please request a new one.");
        }

        LocalDateTime now = LocalDateTime.now();
        String plainResetToken = generateSecureToken();

        entry.setCodeVerified(true);
        entry.setResetTokenHash(hashToken(plainResetToken));
        entry.setResetTokenExpiresAt(now.plusMinutes(resetTokenExpirationMinutes));
        entry.setUpdatedAt(now);

        passwordResetTokenRepository.save(entry);

        String sessionToken = jwtUtils.generatePasswordResetSessionToken(email, plainResetToken, resetTokenExpirationMinutes);

        return new VerifyResetCodeResult(sessionToken, new MessageResponse("Code verified. You can now reset your password."));
    }

    public MessageResponse resetPassword(String sessionToken, ResetPasswordRequest request) {

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException("New password and confirmation do not match");
        }

        JwtUtils.PasswordResetSession session = resolveSession(sessionToken);

        PasswordResetToken entry = resolveValidResetToken(session.email(), session.resetToken());

        User user = userRepository.findByEmail(entry.getEmail())
                .orElseThrow(() -> new UserNotFoundException("No account found for this reset request"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        passwordResetTokenRepository.delete(entry);

        return new MessageResponse("Password reset successfully. You can now log in with your new password.");
    }

    private String resolveRequestEmail(String requestToken) {
        try {
            return jwtUtils.extractPasswordResetRequestEmail(requestToken);
        } catch (Exception e) {
            throw new InvalidResetSessionException("Password reset session expired or invalid. Please request a new code.");
        }
    }

    private JwtUtils.PasswordResetSession resolveSession(String sessionToken) {
        try {
            return jwtUtils.extractPasswordResetSession(sessionToken);
        } catch (Exception e) {
            throw new InvalidResetSessionException("Password reset session expired or invalid. Please restart the process.");
        }
    }

    private PasswordResetToken resolveValidResetToken(String email, String plainResetToken) {

        String tokenHash = hashToken(plainResetToken);

        PasswordResetToken entry = passwordResetTokenRepository.findByResetTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidResetTokenException("Invalid or expired reset token"));

        if (!entry.getEmail().equals(email)) {
            throw new InvalidResetTokenException("Invalid or expired reset token");
        }

        if (!entry.isCodeVerified() || entry.getResetTokenExpiresAt() == null) {
            throw new InvalidResetTokenException("Invalid or expired reset token");
        }

        if (entry.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResetTokenExpiredException("Reset token has expired. Please restart the password reset process.");
        }

        return entry;
    }

    private String generateVerificationCode() {
        int code = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String plainToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}