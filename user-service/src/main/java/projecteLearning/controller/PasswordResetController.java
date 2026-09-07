package projecteLearning.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import projecteLearning.dto.*;
import projecteLearning.exception.InvalidResetSessionException;
import projecteLearning.service.PasswordResetService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private static final String REQUEST_COOKIE_NAME = "password_reset_request";
    private static final String SESSION_COOKIE_NAME = "password_reset_session";

    private final PasswordResetService passwordResetService;

    @Value("${password-reset.code.expiration-minutes:10}")
    private long codeExpirationMinutes;

    @Value("${password-reset.token.expiration-minutes:15}")
    private long resetTokenExpirationMinutes;

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                          HttpServletResponse response) {

        ForgotPasswordResult result = passwordResetService.forgotPassword(request);

        if (result.getRequestToken() != null) {
            setCookie(response, REQUEST_COOKIE_NAME, result.getRequestToken(), codeExpirationMinutes * 60);
        }

        return ResponseEntity.ok(result.getMessage());
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<MessageResponse> verifyResetCode(
            @Valid @RequestBody VerifyResetCodeRequest request,
            @CookieValue(value = REQUEST_COOKIE_NAME, required = false) String requestToken,
            HttpServletResponse response) {

        if (requestToken == null) {
            throw new InvalidResetSessionException("Password reset session expired or invalid. Please request a new code.");
        }

        VerifyResetCodeResult result = passwordResetService.verifyResetCode(requestToken, request);

        clearCookie(response, REQUEST_COOKIE_NAME);
        setCookie(response, SESSION_COOKIE_NAME, result.getSessionToken(), resetTokenExpirationMinutes * 60);

        return ResponseEntity.ok(result.getMessage());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            @CookieValue(value = SESSION_COOKIE_NAME, required = false) String sessionToken,
            HttpServletResponse response) {

        if (sessionToken == null) {
            throw new InvalidResetSessionException("Password reset session expired or invalid. Please restart the process.");
        }

        MessageResponse result = passwordResetService.resetPassword(sessionToken, request);

        clearCookie(response, SESSION_COOKIE_NAME);

        return ResponseEntity.ok(result);
    }

    private void setCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false)
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false)
                .path("/api/auth")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}