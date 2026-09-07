package projecteLearning.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import projecteLearning.dto.*;
import projecteLearning.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String AUTH_COOKIE_NAME = "jwt";
    private static final String VERIFICATION_COOKIE_NAME = "verification_token";

    private final AuthService authService;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${verification.code.expiration-minutes:10}")
    private long verificationExpirationMinutes;

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request,
                                               HttpServletResponse response) {

        SignupResult result = authService.signup(request);
        setVerificationCookie(response, result.getVerificationToken());

        return ResponseEntity.status(HttpStatus.CREATED).body(result.getUser());
    }

    @PostMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,
            @CookieValue(value = VERIFICATION_COOKIE_NAME, required = false) String verificationToken,
            HttpServletResponse response) {

        if (verificationToken == null) {
            throw new projecteLearning.exception.InvalidVerificationSessionException(
                    "Verification session expired. Please sign up again.");
        }

        MessageResponse result = authService.verifyEmail(verificationToken, request);
        clearVerificationCookie(response); // le compte est vérifié, le cookie n'a plus d'utilité

        return ResponseEntity.ok(result);
    }

    @PostMapping("/resend-code")
    public ResponseEntity<MessageResponse> resendCode(
            @CookieValue(value = VERIFICATION_COOKIE_NAME, required = false) String verificationToken,
            HttpServletResponse response) {

        if (verificationToken == null) {
            throw new projecteLearning.exception.InvalidVerificationSessionException(
                    "Verification session expired. Please sign up again.");
        }

        ResendCodeResult result = authService.resendVerificationCode(verificationToken);
        setVerificationCookie(response, result.getVerificationToken()); // cookie réémis avec une expiration fraîche

        return ResponseEntity.ok(result.getMessage());
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@Valid @RequestBody SigninRequest request,
                                    HttpServletResponse response) {

        try {
            SigninResponse signinResponse = authService.signin(request);

            ResponseCookie cookie = ResponseCookie.from(AUTH_COOKIE_NAME, signinResponse.getToken())
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(jwtExpirationMs / 1000)
                    .sameSite("Lax")
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            return ResponseEntity.ok(signinResponse.getUser());

        } catch (projecteLearning.exception.AccountNotVerifiedException ex) {
            setVerificationCookie(response, ex.getVerificationToken());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse(ex.getMessage()));
        }
    }

    /** Sign in (or auto-signup as ETUDIANT) with a Google ID token. See AuthService.googleSignIn(). */
    @PostMapping("/google")
    public ResponseEntity<?> googleSignIn(@Valid @RequestBody GoogleSignInRequest request,
                                          HttpServletResponse response) {

        try {
            SigninResponse signinResponse = authService.googleSignIn(request.getIdToken());

            ResponseCookie cookie = ResponseCookie.from(AUTH_COOKIE_NAME, signinResponse.getToken())
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(jwtExpirationMs / 1000)
                    .sameSite("Lax")
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            return ResponseEntity.ok(signinResponse.getUser());

        } catch (projecteLearning.exception.InvalidGoogleTokenException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse(ex.getMessage()));
        } catch (projecteLearning.exception.GoogleSignInNotAllowedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse(ex.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {

        ResponseCookie cookie = ResponseCookie.from(AUTH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok().build();
    }

    private void setVerificationCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(VERIFICATION_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(false)
                .path("/api/auth")
                .maxAge(verificationExpirationMinutes * 60)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearVerificationCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(VERIFICATION_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .path("/api/auth")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}