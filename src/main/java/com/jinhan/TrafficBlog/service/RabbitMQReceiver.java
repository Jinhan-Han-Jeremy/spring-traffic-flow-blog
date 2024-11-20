package com.jinhan.TrafficBlog.service;

import com.jinhan.TrafficBlog.entity.Article;
import com.jinhan.TrafficBlog.entity.Comment;
import com.jinhan.TrafficBlog.pojo.SendCommentNotification;
import com.jinhan.TrafficBlog.pojo.WriteArticle;
import com.jinhan.TrafficBlog.pojo.WriteComment;
import com.jinhan.TrafficBlog.repository.jpa.ArticleRepository;
import com.jinhan.TrafficBlog.repository.jpa.CommentRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RabbitMQReceiver {
    ArticleRepository articleRepository;
    CommentRepository commentRepository;

    UserNotificationHistoryService userNotificationHistoryService;
    ValidationService validationService;
    RabbitMQSender rabbitMQSender;

    public RabbitMQReceiver(ArticleRepository articleRepository, CommentRepository commentRepository, ValidationService validationService,
                            RabbitMQSender rabbitMQSender, UserNotificationHistoryService userNotificationHistoryService) {
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.validationService = validationService;
        this.rabbitMQSender = rabbitMQSender;
        this.userNotificationHistoryService = userNotificationHistoryService;
    }

    // 이메일 알림 메시지를 처리하는 리스너
    @RabbitListener(queues = "send_notification.email")
    public void emailReceive(String message) {
        System.out.println("Received Message(email): " + message);
    }

    // SMS 알림 메시지를 처리하는 리스너
    @RabbitListener(queues = "send_notification.sms")
    public void smsReceive(String message) {
        System.out.println("Received Message(sms): " + message);
    }

    // 게시글 관련 알림 메시지를 처리하는 리스너
    @RabbitListener(queues = "ad-articles-notification")
    public void receive(String message) {
        System.out.println("Received Message(ad-articles): " + message);
        if (message.contains(WriteComment.class.getSimpleName())) {
            this.sendCommentNotification(message);
            return;
        }
        if (message.contains(WriteArticle.class.getSimpleName())) {
            this.sendArticleNotification(message);
            return;
        }
    }

    // 게시글 작성 알림을 처리하는 메서드
    private void sendArticleNotification(String message) {
        // 메시지를 파싱하여 게시글 ID, 사용자 ID를 추출
        message = message.replace("WriteArticle(", "").replace(")", "");
        String[] parts = message.split(", ");
        String type = null;
        Long articleId = null;
        Long userId = null;

        for (String part : parts) {
            String[] keyValue = part.split("=");
            String key = keyValue[0].trim();
            String value = keyValue[1].trim();

            if (key.equals("type")) {
                type = value;
            } else if (key.equals("articleId")) {
                articleId = Long.parseLong(value);
            } else if (key.equals("userId")) {
                userId = Long.parseLong(value);
            }
        }

        Article article = validationService.validate(articleId, articleRepository, "Article");

        userNotificationHistoryService.insertArticleNotification(article, userId);
    }

    private void sendCommentNotification(String message) {
        // 메시지를 파싱하여 댓글 ID를 추출
        message = message.replace("WriteComment(", "").replace(")", "");

        String[] parts = message.split(", ");
        String type = null;
        Long commentId = null;

        for (String part : parts) {
            String[] keyValue = part.split("=");
            String key = keyValue[0].trim();
            String value = keyValue[1].trim();

            if (key.equals("type")) {
                type = value;
            } else if (key.equals("commentId")) {
                commentId = Long.parseLong(value);
            }
        }

        WriteComment writeComment = new WriteComment();
        writeComment.setType(type);
        writeComment.setCommentId(commentId);

        // 알림 전송
        SendCommentNotification sendCommentNotification = new SendCommentNotification();
        sendCommentNotification.setCommentId(writeComment.getCommentId());

        Comment comment = validationService.validate(writeComment.getCommentId(), commentRepository, "Comment");

        HashSet<Long> userSet = new HashSet<>();  // 중복 방지를 위한 사용자 ID 집합

        // 댓글 작성한 본인
        userSet.add(comment.getAuthor().getId());

        // 글 작성자
        userSet.add(comment.getArticle().getAuthor().getId());

        // 댓글 작성자 모두
        List<Comment> comments = commentRepository.findByArticleId(comment.getArticle().getId());

        for (Comment article_comment : comments) {
            userSet.add(article_comment.getAuthor().getId());
        }

        for (Long userId : userSet) {
            sendCommentNotification.setUserId(userId);
            rabbitMQSender.send(sendCommentNotification);
            userNotificationHistoryService.insertCommentNotification(comment, userId);
        }

    }
}