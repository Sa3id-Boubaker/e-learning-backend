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

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_FORMATEUR = "FORMATEUR";
    private static final String ROLE_ETUDIANT = "ETUDIANT";

    private static final String PATH_COURSES_WILDCARD = "/api/courses/**";
    private static final String PATH_CHAPTERS_WILDCARD = "/api/chapters/**";
    private static final String PATH_VIDEOS_WILDCARD = "/api/videos/**";
    private static final String PATH_COURSES_QUIZ = "/api/courses/*/quiz";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CourseAccessDeniedHandler courseAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/courses").hasAnyRole(ROLE_FORMATEUR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/courses/*/image").hasAnyRole(ROLE_FORMATEUR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, PATH_COURSES_WILDCARD).hasAnyRole(ROLE_FORMATEUR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, PATH_COURSES_WILDCARD).hasAnyRole(ROLE_FORMATEUR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/courses/public").permitAll()
                        .requestMatchers(HttpMethod.GET, PATH_COURSES_WILDCARD).authenticated().requestMatchers(HttpMethod.GET, "/api/courses/summary").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/courses/*/chapters").hasAnyRole(ROLE_FORMATEUR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, PATH_CHAPTERS_WILDCARD).hasAnyRole(ROLE_FORMATEUR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, PATH_CHAPTERS_WILDCARD).hasAnyRole(ROLE_FORMATEUR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/courses/*/chapters", PATH_CHAPTERS_WILDCARD).authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/chapters/*/videos").hasAnyRole(ROLE_FORMATEUR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, PATH_VIDEOS_WILDCARD).hasAnyRole(ROLE_FORMATEUR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, PATH_VIDEOS_WILDCARD).hasAnyRole(ROLE_FORMATEUR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/chapters/*/videos", PATH_VIDEOS_WILDCARD).authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/courses/*/discount").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/courses/*/discount").hasRole(ROLE_ADMIN)
                        .requestMatchers("/api/courses/*/progress", "/api/courses/*/progress/**").hasRole(ROLE_ETUDIANT)
                        // Quiz — la ligne POST est celle qui comble le vrai trou (voir explication en haut de réponse)
                        .requestMatchers(HttpMethod.POST, PATH_COURSES_QUIZ).hasAnyRole(ROLE_FORMATEUR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, PATH_COURSES_QUIZ).hasAnyRole(ROLE_FORMATEUR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, PATH_COURSES_QUIZ).hasAnyRole(ROLE_FORMATEUR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, PATH_COURSES_QUIZ).authenticated()

                        // Questions — chemin entièrement nouveau (/api/quizzes/**), sans ces lignes tout retomberait sur .anyRequest().authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/quizzes/*/questions").hasAnyRole(ROLE_FORMATEUR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/quizzes/*/questions/**").hasAnyRole(ROLE_FORMATEUR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/quizzes/*/questions/**").hasAnyRole(ROLE_FORMATEUR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/quizzes/*/questions").authenticated()

                        // Soumission et résultats — réservés aux étudiants uniquement
                        .requestMatchers(HttpMethod.POST, "/api/quizzes/*/submit").hasRole(ROLE_ETUDIANT)
                        .requestMatchers(HttpMethod.GET, "/api/quizzes/*/my-result", "/api/quizzes/*/my-attempts").hasRole(ROLE_ETUDIANT)

                        .requestMatchers(HttpMethod.POST, "/api/courses/*/certificate").hasRole(ROLE_ETUDIANT)
                        .requestMatchers(HttpMethod.GET, "/api/courses/*/certificate").hasRole(ROLE_ETUDIANT)

                        .requestMatchers(HttpMethod.GET, "/api/certificates/my").hasRole(ROLE_ETUDIANT)
                        .requestMatchers(HttpMethod.GET, "/api/certificates/course/*").hasAnyRole(ROLE_FORMATEUR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/certificates").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/certificates/*").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/enrollments").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/enrollments").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/enrollments/stats/top-courses").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/enrollments/stats").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/enrollments/stats/my-courses").hasRole(ROLE_FORMATEUR)
                        .requestMatchers(HttpMethod.DELETE, "/api/enrollments/**").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/enrollments/my").hasRole(ROLE_ETUDIANT)
                        .requestMatchers(HttpMethod.GET, "/api/courses/*/enrollments").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/courses/*/access").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/enrollments/my/courses").hasRole(ROLE_ETUDIANT)
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.accessDeniedHandler(courseAccessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}