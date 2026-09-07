package com.test.notificationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "omarise.events";
    public static final String QUEUE_ENROLLMENT_ACTIVATED = "notification.enrollment.activated";
    public static final String ROUTING_KEY_ENROLLMENT_ACTIVATED = "enrollment.activated";

    public static final String QUEUE_TRAINING_ENROLLMENT_ACTIVATED = "notification.training-enrollment.activated";
    public static final String ROUTING_KEY_TRAINING_ENROLLMENT_ACTIVATED = "training-enrollment.activated";

    public static final String QUEUE_FORUM_TRAINING_POST_CREATED = "notification.forum.training-post.created";
    public static final String ROUTING_KEY_FORUM_TRAINING_POST_CREATED = "forum.training-post.created";

    public static final String QUEUE_FORUM_POST_COMMENT_CREATED = "notification.forum.post-comment.created";
    public static final String ROUTING_KEY_FORUM_POST_COMMENT_CREATED = "forum.post-comment.created";

    @Bean
    public TopicExchange omariseEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue enrollmentActivatedQueue() {
        return new Queue(QUEUE_ENROLLMENT_ACTIVATED, true);
    }

    @Bean
    public Binding enrollmentActivatedBinding(Queue enrollmentActivatedQueue, TopicExchange omariseEventsExchange) {
        return BindingBuilder.bind(enrollmentActivatedQueue).to(omariseEventsExchange).with(ROUTING_KEY_ENROLLMENT_ACTIVATED);
    }

    @Bean
    public Queue trainingEnrollmentActivatedQueue() {
        return new Queue(QUEUE_TRAINING_ENROLLMENT_ACTIVATED, true);
    }

    @Bean
    public Binding trainingEnrollmentActivatedBinding(Queue trainingEnrollmentActivatedQueue, TopicExchange omariseEventsExchange) {
        return BindingBuilder.bind(trainingEnrollmentActivatedQueue).to(omariseEventsExchange).with(ROUTING_KEY_TRAINING_ENROLLMENT_ACTIVATED);
    }

    @Bean
    public Queue forumTrainingPostCreatedQueue() {
        return new Queue(QUEUE_FORUM_TRAINING_POST_CREATED, true);
    }

    @Bean
    public Binding forumTrainingPostCreatedBinding(Queue forumTrainingPostCreatedQueue, TopicExchange omariseEventsExchange) {
        return BindingBuilder.bind(forumTrainingPostCreatedQueue).to(omariseEventsExchange).with(ROUTING_KEY_FORUM_TRAINING_POST_CREATED);
    }

    @Bean
    public Queue forumPostCommentCreatedQueue() {
        return new Queue(QUEUE_FORUM_POST_COMMENT_CREATED, true);
    }

    @Bean
    public Binding forumPostCommentCreatedBinding(Queue forumPostCommentCreatedQueue, TopicExchange omariseEventsExchange) {
        return BindingBuilder.bind(forumPostCommentCreatedQueue).to(omariseEventsExchange).with(ROUTING_KEY_FORUM_POST_COMMENT_CREATED);
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, JacksonJsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        return factory;
    }
}