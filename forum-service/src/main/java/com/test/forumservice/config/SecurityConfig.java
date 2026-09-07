package com.test.forumservice.config;

import com.test.forumservice.security.ForumAccessDeniedHandler;
import com.test.forumservice.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Toutes les routes du forum exigent simplement authenticated() : n'importe
 * quel rôle (ADMIN / FORMATEUR / ETUDIANT) peut créer un post/commentaire,
 * bookmark, upvote. Les contrôles fins "auteur ou admin" pour l'édition et
 * la suppression sont faits en couche service via ForumAccessDeniedException,
 * pas ici — c'est la même approche que TRAINING-SERVICE pour les mutations
 * qui dépendent de la propriété d'une ressource plutôt que d'un rôle fixe.
 *
 * Exception : /api/forum/comments/count (dashboard admin) est restreint à
 * ADMIN au niveau route, puisque ce n'est pas un contrôle par propriété.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ForumAccessDeniedHandler forumAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/forum/comments/count").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(forumAccessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}