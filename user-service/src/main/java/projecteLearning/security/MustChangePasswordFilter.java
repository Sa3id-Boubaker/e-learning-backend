package projecteLearning.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class MustChangePasswordFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            boolean mustChange = Boolean.TRUE.equals(principal.getUser().getMustChangePassword());

            if (mustChange && !isAllowedWhileMustChange(request)) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                String json = """
                        {"timestamp":"%s","status":403,"message":"You must change your password before continuing."}""".formatted(LocalDateTime.now());

                response.getWriter().write(json);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedWhileMustChange(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (uri.equals("/api/users/me/force-change-password") && method.equals("PUT")) return true;
        if (uri.equals("/api/auth/logout") && method.equals("POST")) return true;
        if (uri.equals("/api/users/me") && method.equals("GET")) return true;

        // Laisse Swagger accessible pendant vos tests — sans ça, un compte
        // "mustChangePassword" ne pourrait même plus ouvrir la doc pour se tester lui-même.
        if (uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs")) return true;

        return false;
    }
}