package com.test.forumservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publisher-side only (forum-service never consumes) — mirrors training-service's RabbitMQConfig
 * exactly: same exchange name/properties (durable=true, autoDelete=false, topic), no Queue/Binding
 * declared here (queue ownership belongs to the consumer, notification-service). The TopicExchange
 * bean MUST match byte-for-byte across every service that declares it, or RabbitMQ rejects the
 * redeclare with PRECONDITION_FAILED at broker level.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "omarise.events";
    public static final String ROUTING_KEY_FORUM_TRAINING_POST_CREATED = "forum.training-post.created";
    public static final String ROUTING_KEY_FORUM_POST_COMMENT_CREATED = "forum.post-comment.created";

    @Bean
    public TopicExchange omariseEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, JacksonJsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}