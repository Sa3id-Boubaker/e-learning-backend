package projecteLearning.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import projecteLearning.dto.*;
import projecteLearning.exception.*;
import projecteLearning.models.Role;
import projecteLearning.models.User;
import projecteLearning.repository.UserRepository;
import projecteLearning.security.JwtUtils;
import projecteLearning.security.UserPrincipal;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    @Value("${verification.code.expiration-minutes:10}")
    private long verificationExpirationMinutes;

    public SignupResult signup(SignupRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("This email is already registered: " + request.getEmail());
        }

        LocalDateTime now = LocalDateTime.now();
        String plainVerificationCode = generateVerificationCode();

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.ETUDIANT)
                .enabled(false)
                .mustChangePassword(false)
                .verificationCodeHash(passwordEncoder.encode(plainVerificationCode))
                .verificationCodeExpiresAt(now.plusMinutes(verificationExpirationMinutes))
                .createdAt(now)
                .updatedAt(now)
                .build();

        User savedUser = userRepository.save(user);

        emailService.sendVerificationCode(savedUser.getEmail(), savedUser.getFirstName(), plainVerificationCode, verificationExpirationMinutes);

        String verificationToken = jwtUtils.generateVerificationToken(savedUser.getEmail());

        return new SignupResult(verificationToken, toUserResponse(savedUser));
    }

    public MessageResponse verifyEmail(String verificationToken, VerifyEmailRequest request) {

        String email = resolveVerificationEmail(verificationToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("No account found with this email"));

        if (Boolean.TRUE.equals(user.getEnabled())) {
            throw new AccountAlreadyVerifiedException("This account is already verified");
        }

        if (user.getVerificationCodeHash() == null
                || !passwordEncoder.matches(request.getCode(), user.getVerificationCodeHash())) {
            throw new InvalidVerificationCodeException("Invalid verification code");
        }

        if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new VerificationCodeExpiredException("Verification code has expired. Please request a new one.");
        }

        user.setEnabled(true);
        user.setVerificationCodeHash(null);
        user.setVerificationCodeExpiresAt(null);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        return new MessageResponse("Account verified successfully. You can now log in.");
    }

    public ResendCodeResult resendVerificationCode(String verificationToken) {

        String email = resolveVerificationEmail(verificationToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("No account found with this email"));

        if (Boolean.TRUE.equals(user.getEnabled())) {
            throw new AccountAlreadyVerifiedException("This account is already verified");
        }

        String newVerificationToken = generateAndSendVerificationCode(user);

        return new ResendCodeResult(newVerificationToken, new MessageResponse("A new verification code has been sent to your email."));
    }

    public SigninResponse signin(SigninRequest request) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            User user = principal.getUser();

            String token = jwtUtils.generateToken(user.getId(), user.getEmail(), user.getRole().name());

            return new SigninResponse(token, toUserResponse(user));

        } catch (DisabledException e) {
            // Le compte existe mais n'est pas encore vérifié (enabled = false).
            // Spring Security vérifie ce statut AVANT le mot de passe, donc on le
            // revérifie nous-mêmes ici avant de renvoyer un code, pour éviter que
            // n'importe qui puisse déclencher un envoi d'email juste avec un email valide.
            throw handleUnverifiedSignin(request);
        }
    }

    /**
     * Sign-in / auto-signup via Google. Only ever produces/logs into an ETUDIANT account —
     * mirrors the "students only" rule already enforced in signup(). Google already verifies
     * the email itself, so there is no separate email-verification-code step here.
     */
    public SigninResponse googleSignIn(String idToken) {

        GoogleIdToken.Payload payload = verifyGoogleToken(idToken).getPayload();

        String email = payload.getEmail();
        boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());

        if (email == null || !emailVerified) {
            throw new InvalidGoogleTokenException("Google account email is missing or not verified.");
        }

        String firstName = (String) payload.get("given_name");
        String lastName = (String) payload.get("family_name");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = createGoogleUser(email, firstName, lastName);
        } else if (user.getRole() != Role.ETUDIANT) {
            // Google sign-in is offered to students only — never let it become a shortcut
            // into an existing FORMATEUR/ADMIN account.
            throw new GoogleSignInNotAllowedException(
                    "This email is already registered as " + user.getRole() + ". Please sign in with your password.");
        } else if (!Boolean.TRUE.equals(user.getEnabled())) {
            // Local ETUDIANT account that never finished email verification — Google just
            // proved the email is real, so finish it here instead of blocking the sign-in.
            user.setEnabled(true);
            user.setVerificationCodeHash(null);
            user.setVerificationCodeExpiresAt(null);
            user.setUpdatedAt(LocalDateTime.now());
            user = userRepository.save(user);
        }

        String token = jwtUtils.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new SigninResponse(token, toUserResponse(user));
    }

    private User createGoogleUser(String email, String firstName, String lastName) {
        LocalDateTime now = LocalDateTime.now();

        // A random, never-issued password — still needs a valid bcrypt hash in `password`
        // (not null) so a later classic /api/auth/signin attempt on this email fails cleanly
        // with "invalid credentials" instead of crashing on a null encoded password.
        String unusablePassword = passwordEncoder.encode(UUID.randomUUID().toString());

        User user = User.builder()
                .firstName(firstName != null ? firstName : "")
                .lastName(lastName != null ? lastName : "")
                .email(email)
                .password(unusablePassword)
                .role(Role.ETUDIANT)
                .enabled(true) // Google already verified this email
                .mustChangePassword(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return userRepository.save(user);
    }

    private GoogleIdToken verifyGoogleToken(String idToken) {
        try {
            GoogleIdToken googleIdToken = googleIdTokenVerifier.verify(idToken);
            if (googleIdToken == null) {
                throw new InvalidGoogleTokenException("Invalid or expired Google token.");
            }
            return googleIdToken;
        } catch (InvalidGoogleTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidGoogleTokenException("Could not verify Google token: " + e.getMessage());
        }
    }

    private RuntimeException handleUnverifiedSignin(SigninRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String newVerificationToken = generateAndSendVerificationCode(user);

        return new AccountNotVerifiedException(newVerificationToken,
                "Your account is not verified yet. A new verification code has been sent to your email.");
    }

    /** Génère un nouveau code, le hash et le persiste sur l'utilisateur, envoie l'email, et renvoie le nouveau token de vérification. */
    private String generateAndSendVerificationCode(User user) {

        String plainCode = generateVerificationCode();

        user.setVerificationCodeHash(passwordEncoder.encode(plainCode));
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(verificationExpirationMinutes));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        emailService.sendVerificationCode(user.getEmail(), user.getFirstName(), plainCode, verificationExpirationMinutes);

        return jwtUtils.generateVerificationToken(user.getEmail());
    }

    private String resolveVerificationEmail(String verificationToken) {
        try {
            return jwtUtils.extractVerificationEmail(verificationToken);
        } catch (Exception e) {
            throw new InvalidVerificationSessionException("Verification session expired or invalid. Please sign up again.");
        }
    }

    private String generateVerificationCode() {
        int code = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .phone(user.getPhone())
                .enabled(user.getEnabled())
                .mustChangePassword(user.getMustChangePassword())
                .createdAt(user.getCreatedAt())
                .build();
    }
}