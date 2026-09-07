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
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/trainings").hasAnyRole("ADMIN", "FORMATEUR")
                        .requestMatchers(HttpMethod.PUT, "/api/trainings/**").hasAnyRole("ADMIN", "FORMATEUR")
                        .requestMatchers(HttpMethod.DELETE, "/api/trainings/**").hasAnyRole("ADMIN", "FORMATEUR")
                        .requestMatchers(HttpMethod.POST, "/api/trainings/*/image").hasAnyRole("ADMIN", "FORMATEUR")
                        .requestMatchers(HttpMethod.GET, "/api/trainings/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/trainings/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/trainings/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/trainings/*/sessions").hasAnyRole("ADMIN", "FORMATEUR")
                        .requestMatchers(HttpMethod.GET, "/api/trainings/*/sessions").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/sessions/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/sessions/**").hasAnyRole("ADMIN", "FORMATEUR")
                        .requestMatchers(HttpMethod.DELETE, "/api/sessions/**").hasAnyRole("ADMIN", "FORMATEUR")
                        .requestMatchers(HttpMethod.POST, "/api/sessions/*/recording").hasAnyRole("ADMIN", "FORMATEUR")
                        .requestMatchers(HttpMethod.PUT, "/api/recordings/**").hasAnyRole("ADMIN", "FORMATEUR")
                        .requestMatchers(HttpMethod.POST, "/api/recordings/*/video").hasAnyRole("ADMIN", "FORMATEUR")
                        .requestMatchers(HttpMethod.DELETE, "/api/recordings/**").hasAnyRole("ADMIN", "FORMATEUR")
                        .requestMatchers(HttpMethod.GET, "/api/training-enrollments/my").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/training-enrollments").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/training-enrollments").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/training-enrollments/stats/popular-trainings").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/training-enrollments/stats/top-trainings").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/training-enrollments/stats/my-trainings").hasRole("FORMATEUR")
                        .requestMatchers(HttpMethod.GET, "/api/training-enrollments/stats").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/training-enrollments/stats/top-trainings").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/training-enrollments/stats").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/training-enrollments/stats").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/training-enrollments/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.accessDeniedHandler(trainingAccessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}