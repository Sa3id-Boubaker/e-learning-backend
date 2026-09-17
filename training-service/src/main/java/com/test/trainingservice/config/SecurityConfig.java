package com.test.trainingservice.config;

import com.test.trainingservice.security.JwtAuthenticationFilter;
import com.test.trainingservice.security.TrainingAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_FORMATEUR = "FORMATEUR";
    private static final String PATH_TRAININGS = "/api/trainings/**";
    private static final String PATH_SESSIONS = "/api/sessions/**";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TrainingAccessDeniedHandler trainingAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/trainings").hasAnyRole(ROLE_ADMIN, ROLE_FORMATEUR)
                        .requestMatchers(HttpMethod.PUT, PATH_TRAININGS).hasAnyRole(ROLE_ADMIN, ROLE_FORMATEUR)
                        .requestMatchers(HttpMethod.DELETE, PATH_TRAININGS).hasAnyRole(ROLE_ADMIN, ROLE_FORMATEUR)
                        .requestMatchers(HttpMethod.POST, "/api/trainings/*/image").hasAnyRole(ROLE_ADMIN, ROLE_FORMATEUR)
                        .requestMatchers(HttpMethod.GET, "/api/trainings/public").permitAll()
                        .requestMatchers(HttpMethod.GET, PATH_TRAININGS).authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/trainings/*/sessions").hasAnyRole(ROLE_ADMIN, ROLE_FORMATEUR)
                        .requestMatchers(HttpMethod.GET, "/api/trainings/*/sessions").authenticated()
                        .requestMatchers(HttpMethod.GET, PATH_SESSIONS).authenticated()
                        .requestMatchers(HttpMethod.PUT, PATH_SESSIONS).hasAnyRole(ROLE_ADMIN, ROLE_FORMATEUR)
                        .requestMatchers(HttpMethod.DELETE, PATH_SESSIONS).hasAnyRole(ROLE_ADMIN, ROLE_FORMATEUR)
                        .requestMatchers(HttpMethod.POST, "/api/sessions/*/recording").hasAnyRole(ROLE_ADMIN, ROLE_FORMATEUR)
                        .requestMatchers(HttpMethod.PUT, "/api/recordings/**").hasAnyRole(ROLE_ADMIN, ROLE_FORMATEUR)
                        .requestMatchers(HttpMethod.POST, "/api/recordings/*/video").hasAnyRole(ROLE_ADMIN, ROLE_FORMATEUR)
                        .requestMatchers(HttpMethod.DELETE, "/api/recordings/**").hasAnyRole(ROLE_ADMIN, ROLE_FORMATEUR)
                        .requestMatchers(HttpMethod.GET, "/api/training-enrollments/my").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/training-enrollments").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/training-enrollments").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/training-enrollments/stats/popular-trainings").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/training-enrollments/stats/top-trainings").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/training-enrollments/stats/my-trainings").hasRole(ROLE_FORMATEUR)
                        .requestMatchers(HttpMethod.GET, "/api/training-enrollments/stats").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/training-enrollments/**").hasRole(ROLE_ADMIN)
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.accessDeniedHandler(trainingAccessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}