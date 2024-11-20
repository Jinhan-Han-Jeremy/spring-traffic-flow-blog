package com.jinhan.TrafficBlog.config;


import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue notificationQueue() {
        return new Queue("send_notification.email", true);
    }

    @Bean
    public Queue smsQueue() {
        return new Queue("send_notification.sms", true); // true ensures the queue is durable
    }

    @Bean
    public Queue adArticlesNotificationQueue() {
        return new Queue("ad-articles-notification", true);
    }
}