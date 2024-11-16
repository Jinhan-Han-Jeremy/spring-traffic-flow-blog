package com.jinhan.TrafficBlog.service;

import com.jinhan.TrafficBlog.pojo.WriteArticle;
import com.jinhan.TrafficBlog.pojo.SendCommentNotification;
import com.jinhan.TrafficBlog.pojo.WriteComment;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQSender {
    private final RabbitTemplate rabbitTemplate;

    public RabbitMQSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(WriteArticle articleNotification) {
        rabbitTemplate.convertAndSend("ad-articles-notification", articleNotification.toString());
    }

    public void send(WriteComment message) {
        rabbitTemplate.convertAndSend("ad-articles-notification", message.toString());
    }

    public void send(SendCommentNotification message) {
        rabbitTemplate.convertAndSend("send_notification_exchange", "", message.toString());
    }
}