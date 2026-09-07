package com.test.trainingservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * Builder "normal" (non load-balancé), marqué @Primary : c'est celui que reçoit
     * tout composant qui injecte RestClient.Builder SANS qualifier explicite — notamment
     * le client Eureka interne (Spring Cloud Netflix Eureka Client 5.0.2 utilise RestClient
     * en interne et l'injecte sans qualifier). Sans @Primary, Spring ne peut plus choisir
     * entre ce bean et loadBalancedRestClientBuilder ci-dessous, et Eureka plante
     * (NoUniqueBeanDefinitionException) dès qu'il essaie de s'enregistrer / battre son coeur.
     */
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    /**
     * Builder load-balancé, utilisé UNIQUEMENT par UserServiceClient (via @Qualifier
     * explicite, qui prime toujours sur @Primary) pour résoudre "http://USER-SERVICE"
     * via Eureka + Spring Cloud LoadBalancer.
     */
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}