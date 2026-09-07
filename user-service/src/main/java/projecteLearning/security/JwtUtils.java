package projecteLearning.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtils {

    private static final String VERIFICATION_PURPOSE = "EMAIL_VERIFICATION";

    private static final String PASSWORD_RESET_REQUEST_PURPOSE = "PASSWORD_RESET_REQUEST";
    private static final String PASSWORD_RESET_SESSION_PURPOSE = "PASSWORD_RESET_SESSION";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMs;

    @Value("${verification.code.expiration-minutes:10}")
    private long verificationExpirationMinutes;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // --- Token de session (signin) ---

    public String generateToken(String userId, String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    // --- Token de session de vérification d'email (signup / verify-email / resend-code) ---

    public String generateVerificationToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + verificationExpirationMinutes * 60_000);

        return Jwts.builder()
                .subject(email)
                .claim("purpose", VERIFICATION_PURPOSE)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }


    public String generatePasswordResetRequestToken(String email, long expirationMinutes) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMinutes * 60_000);

        return Jwts.builder()
                .subject(email)
                .claim("purpose", PASSWORD_RESET_REQUEST_PURPOSE)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractPasswordResetRequestEmail(String token) {
        Claims claims = extractClaims(token);
        String purpose = claims.get("purpose", String.class);
        if (!PASSWORD_RESET_REQUEST_PURPOSE.equals(purpose)) {
            throw new JwtException("Not a password reset request token");
        }
        return claims.getSubject();
    }

    public String generatePasswordResetSessionToken(String email, String resetToken, long expirationMinutes) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMinutes * 60_000);

        return Jwts.builder()
                .subject(email)
                .claim("purpose", PASSWORD_RESET_SESSION_PURPOSE)
                .claim("resetToken", resetToken)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public PasswordResetSession extractPasswordResetSession(String token) {
        Claims claims = extractClaims(token);
        String purpose = claims.get("purpose", String.class);
        if (!PASSWORD_RESET_SESSION_PURPOSE.equals(purpose)) {
            throw new JwtException("Not a password reset session token");
        }
        return new PasswordResetSession(claims.getSubject(), claims.get("resetToken", String.class));
    }

    public record PasswordResetSession(String email, String resetToken) {}

    /** Lève une exception si le token est absent, invalide, expiré, ou n'est pas un token de vérification. */
    public String extractVerificationEmail(String token) {
        Claims claims = extractClaims(token);

        String purpose = claims.get("purpose", String.class);
        if (!VERIFICATION_PURPOSE.equals(purpose)) {
            throw new JwtException("Not a verification token");
        }
        return claims.getSubject();
    }

    public long getVerificationExpirationMinutes() {
        return verificationExpirationMinutes;
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}