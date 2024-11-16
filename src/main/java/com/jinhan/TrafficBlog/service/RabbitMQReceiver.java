package com.jinhan.TrafficBlog.service;

import com.jinhan.TrafficBlog.entity.Article;
import com.jinhan.TrafficBlog.entity.Comment;
import com.jinhan.TrafficBlog.pojo.SendCommentNotification;
import com.jinhan.TrafficBlog.pojo.WriteArticle;
import com.jinhan.TrafficBlog.pojo.WriteComment;
import com.jinhan.TrafficBlog.repository.ArticleRepository;
import com.jinhan.TrafficBlog.repository.CommentRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RabbitMQReceiver {
    ArticleRepository articleRepository;
    CommentRepository commentRepository;
    UserNotificationHistoryService userNotificationHistoryService;

    RabbitMQSender rabbitMQSender;

    public RabbitMQReceiver(ArticleRepository articleRepository, CommentRepository commentRepository, RabbitMQSender rabbitMQSender,
                            UserNotificationHistoryService userNotificationHistoryService) {
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.rabbitMQSender = rabbitMQSender;
        this.userNotificationHistoryService = userNotificationHistoryService;
    }

    @RabbitListener(queues = "send_notification.email")
    public void emailReceive(String message) {
        System.out.println("Received Message(email): " + message);
    }

    @RabbitListener(queues = "send_notification.sms")
    public void smsReceive(String message) {
        System.out.println("Received Message(sms): " + message);
    }

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

    private void sendArticleNotification(String message) {
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

        Optional<Article> article = articleRepository.findById(articleId);
        if (article.isPresent()) {
            userNotificationHistoryService.insertArticleNotification(article.get(), userId);
        }
    }

    private void sendCommentNotification(String message) {
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
        Optional<Comment> comment = commentRepository.findById(writeComment.getCommentId());
        if (comment.isEmpty()) {
            return;
        }

        HashSet<Long> userSet = new HashSet<>();
        // 댓글 작성한 본인
        userSet.add(comment.get().getAuthor().getId());
        // 글 작성자
        userSet.add(comment.get().getArticle().getAuthor().getId());
        // 댓글 작성자 모두
        List<Comment> comments = commentRepository.findByArticleId(comment.get().getArticle().getId());
        for (Comment article_comment : comments) {
            userSet.add(article_comment.getAuthor().getId());
        }
        for (Long userId : userSet) {
            sendCommentNotification.setUserId(userId);
            rabbitMQSender.send(sendCommentNotification);
            userNotificationHistoryService.insertCommentNotification(comment.get(), userId);
        }
    }
}