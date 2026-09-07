package com.test.courseservice.config;

import com.test.courseservice.security.CourseAccessDeniedHandler;
import com.test.courseservice.security.JwtAuthenticationFilter;
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
    private final CourseAccessDeniedHandler courseAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/courses").hasAnyRole("FORMATEUR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/courses/*/image").hasAnyRole("FORMATEUR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/courses/**").hasAnyRole("FORMATEUR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/courses/**").hasAnyRole("FORMATEUR", "ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/courses/public").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/courses/**").authenticated().requestMatchers(HttpMethod.GET, "/api/courses/summary").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/courses/**").authenticated().requestMatchers(HttpMethod.GET, "/api/courses/summary").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/courses/*/chapters").hasAnyRole("FORMATEUR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/chapters/**").hasAnyRole("FORMATEUR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/chapters/**").hasAnyRole("FORMATEUR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/courses/*/chapters", "/api/chapters/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/chapters/*/videos").hasAnyRole("FORMATEUR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/videos/**").hasAnyRole("FORMATEUR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/videos/**").hasAnyRole("FORMATEUR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/chapters/*/videos", "/api/videos/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/courses/*/discount").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/courses/*/discount").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/courses").hasAnyRole("FORMATEUR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/courses/**").hasAnyRole("FORMATEUR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/courses/**").hasAnyRole("FORMATEUR", "ADMIN")
                        .requestMatchers("/api/courses/*/progress", "/api/courses/*/progress/**").hasRole("ETUDIANT")
                                // Quiz — la ligne POST est celle qui comble le vrai trou (voir explication en haut de réponse)
                                .requestMatchers(HttpMethod.POST, "/api/courses/*/quiz").hasAnyRole("FORMATEUR", "ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/api/courses/*/quiz").hasAnyRole("FORMATEUR", "ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/courses/*/quiz").hasAnyRole("FORMATEUR", "ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/courses/*/quiz").authenticated()

// Questions — chemin entièrement nouveau (/api/quizzes/**), sans ces lignes tout retomberait sur .anyRequest().authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/quizzes/*/questions").hasAnyRole("FORMATEUR", "ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/api/quizzes/*/questions/**").hasAnyRole("FORMATEUR", "ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/quizzes/*/questions/**").hasAnyRole("FORMATEUR", "ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/quizzes/*/questions").authenticated()

// Soumission et résultats — réservés aux étudiants uniquement
                                .requestMatchers(HttpMethod.POST, "/api/quizzes/*/submit").hasRole("ETUDIANT")
                                .requestMatchers(HttpMethod.GET, "/api/quizzes/*/my-result", "/api/quizzes/*/my-attempts").hasRole("ETUDIANT")

                                .requestMatchers(HttpMethod.POST, "/api/courses/*/certificate").hasRole("ETUDIANT")
                                .requestMatchers(HttpMethod.GET, "/api/courses/*/certificate").hasRole("ETUDIANT")

                                .requestMatchers(HttpMethod.GET, "/api/certificates/my").hasRole("ETUDIANT")
                                .requestMatchers(HttpMethod.GET, "/api/certificates/course/*").hasAnyRole("FORMATEUR", "ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/certificates").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/certificates/*").permitAll()

                                .requestMatchers(HttpMethod.POST, "/api/enrollments").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/enrollments").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/enrollments/stats/top-courses").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/enrollments/stats").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/enrollments/stats").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/enrollments/stats/my-courses").hasRole("FORMATEUR")
                                .requestMatchers(HttpMethod.DELETE, "/api/enrollments/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/enrollments/my").hasRole("ETUDIANT")
                                .requestMatchers(HttpMethod.GET, "/api/courses/*/enrollments").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/courses/*/access").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/enrollments/my/courses").hasRole("ETUDIANT")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.accessDeniedHandler(courseAccessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}