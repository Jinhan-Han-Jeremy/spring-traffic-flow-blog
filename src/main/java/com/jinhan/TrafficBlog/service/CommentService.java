package com.jinhan.TrafficBlog.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinhan.TrafficBlog.dto.WriteCommentDto;
import com.jinhan.TrafficBlog.entity.*;
import com.jinhan.TrafficBlog.exception.ForbiddenException;
import com.jinhan.TrafficBlog.exception.RateLimitException;
import com.jinhan.TrafficBlog.exception.ResourceNotFoundException;
import com.jinhan.TrafficBlog.pojo.WriteComment;
import com.jinhan.TrafficBlog.repository.jpa.ArticleRepository;
import com.jinhan.TrafficBlog.repository.jpa.BoardRepository;
import com.jinhan.TrafficBlog.repository.jpa.CommentRepository;
import com.jinhan.TrafficBlog.repository.jpa.UserRepository;
import com.jinhan.TrafficBlog.task.DailyHotArticleTasks;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class CommentService {
    private final BoardRepository boardRepository;
    private final ArticleRepository articleRepository;

    private final CommentRepository commentRepository;

    private final UserRepository userRepository;

    private final ValidationService validationService;

    private final ElasticSearchService elasticSearchService;

    private final ObjectMapper objectMapper;

    private final RabbitMQSender rabbitMQSender;

    private RedisTemplate<String, Object> redisTemplate;

    //각 동일한 데이터 타입 형태의 bean을 사용하지 않아서 Qualifier를 선언하지 않아도 된다
    @Autowired
    public CommentService(BoardRepository boardRepository, ArticleRepository articleRepository, UserRepository userRepository, CommentRepository commentRepository,
                          ValidationService validationService, ElasticSearchService elasticSearchService, ObjectMapper objectMapper,
                          RabbitMQSender rabbitMQSender, RedisTemplate<String, Object> redisTemplate) {

        this.boardRepository = boardRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.validationService = validationService;
        this.elasticSearchService = elasticSearchService;
        this.objectMapper = objectMapper;
        this.rabbitMQSender = rabbitMQSender;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public Comment writeComment(Long boardId, Long articleId, WriteCommentDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        if (!this.isCanWriteComment()) {
            throw new RateLimitException("comment not written by rate limit");
        }

        User author = validationService.currentUser(userRepository);

        validationService.validate(boardId, boardRepository, "Board");

        Article article = validationService.validate(articleId, articleRepository, "Article");


        if (article.getIsDeleted()) {
            throw new ForbiddenException("article is deleted");
        }
        Comment comment = new Comment();
        comment.setArticle(article);
        comment.setAuthor(author);
        comment.setContent(dto.getContent());
        commentRepository.save(comment);
        WriteComment writeComment = new WriteComment();
        writeComment.setCommentId(comment.getId());
        rabbitMQSender.send(writeComment);
        return comment;
    }

    @Transactional
    public Comment editComment(Long boardId, Long articleId, Long commentId, WriteCommentDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (!this.isCanEditComment()) {
            throw new RateLimitException("comment not written by rate limit");
        }

        User author = validationService.currentUser(userRepository);
        validationService.validate(boardId, boardRepository, "Board");
        Article article = validationService.validate(articleId, articleRepository, "Article");

        if (article.getIsDeleted()) {
            throw new ForbiddenException("article is deleted");
        }

        Comment comment = validationService.validate(commentId, commentRepository, "Comment");

        if (comment.getAuthor() != author) {
            throw new ForbiddenException("comment author different");
        }

        if (dto.getContent() != null) {
            comment.setContent(dto.getContent());
        }

        commentRepository.save(comment);
        return comment;
    }

    public boolean deleteComment(Long boardId, Long articleId, Long commentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        if (!this.isCanEditComment()) {
            throw new RateLimitException("comment not written by rate limit");
        }

        User author = validationService.currentUser(userRepository);
        validationService.validate(boardId, boardRepository, "Board");
        Article article = validationService.validate(articleId, articleRepository, "Article");

        if (article.getIsDeleted()) {
            throw new ForbiddenException("article is deleted");
        }

        Comment comment = validationService.validate(commentId, commentRepository, "Comment");

        if (comment.getIsDeleted()) {
            throw new ResourceNotFoundException("comment is deleted");
        }

        if (comment.getAuthor() != author) {
            throw new ForbiddenException("comment author different");
        }

        comment.setIsDeleted(true);
        commentRepository.save(comment);
        return true;
    }

    private boolean isCanWriteComment() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Comment latestComment = commentRepository.findLatestCommentOrderByCreatedDate(userDetails.getUsername());
        if (latestComment == null) {
            return true;
        }
        return this.isDifferenceMoreThanOneMinutes(latestComment.getCreatedDate());
    }

    private boolean isCanEditComment() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Comment latestComment = commentRepository.findLatestCommentOrderByCreatedDate(userDetails.getUsername());
        if (latestComment == null || latestComment.getUpdatedDate() == null) {
            return true;
        }
        return this.isDifferenceMoreThanOneMinutes(latestComment.getUpdatedDate());
    }

    private boolean isDifferenceMoreThanOneMinutes(LocalDateTime localDateTime) {
        LocalDateTime dateAsLocalDateTime = new Date().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        Duration duration = Duration.between(localDateTime, dateAsLocalDateTime);

        return Math.abs(duration.toMinutes()) > 1;
    }

    @Async
    @Transactional
    protected CompletableFuture<Article> getArticle(Long boardId, Long articleId) throws JsonProcessingException {
        Object yesterdayHotArticleTempObj = redisTemplate.opsForHash().get(DailyHotArticleTasks.YESTERDAY_REDIS_KEY + articleId, articleId);
        Object weekHotArticleTempObj = redisTemplate.opsForHash().get(DailyHotArticleTasks.WEEK_REDIS_KEY + articleId, articleId);

        if (yesterdayHotArticleTempObj != null || weekHotArticleTempObj != null) {
            HotArticle hotArticle = (HotArticle) (yesterdayHotArticleTempObj != null ? yesterdayHotArticleTempObj : weekHotArticleTempObj);
            Article article = new Article();
            article.setId(hotArticle.getId());
            article.setTitle(hotArticle.getTitle());
            article.setContent(hotArticle.getContent());
            User user = new User();
            user.setUsername(hotArticle.getAuthorName());
            article.setAuthor(user);
            article.setCreatedDate(hotArticle.getCreatedDate());
            article.setUpdatedDate(hotArticle.getUpdatedDate());
            article.setViewCount(hotArticle.getViewCount());
            return CompletableFuture.completedFuture(article);
        }

        validationService.validate(boardId, boardRepository, "Board");

        Article article = validationService.validate(articleId, articleRepository, "Article");
        if (article.getIsDeleted()) {
            throw new ResourceNotFoundException("article not found");
        }

        article.setViewCount(article.getViewCount() + 1);
        articleRepository.save(article);
        String articleJson = objectMapper.writeValueAsString(article);
        elasticSearchService.indexArticleDocument(article.getId().toString(), articleJson).block();
        return CompletableFuture.completedFuture(article);
    }

    @Async
    protected CompletableFuture<List<Comment>> getComments(Long articleId) {
        return CompletableFuture.completedFuture(commentRepository.findByArticleId(articleId));
    }

    public CompletableFuture<Article> getArticleWithComment(Long boardId, Long articleId) throws JsonProcessingException {
        CompletableFuture<Article> articleFuture = this.getArticle(boardId, articleId);
        CompletableFuture<List<Comment>> commentsFuture = this.getComments(articleId);

        return CompletableFuture.allOf(articleFuture, commentsFuture)
                .thenApply(voidResult -> {
                    try {
                        Article article = articleFuture.get();
                        List<Comment> comments = commentsFuture.get();
                        article.setComments(comments);
                        return article;
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        return null;
                    }
                });
    }
}
