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
    // 필요한 Repository와 Service 의존성 주입을 위한 필드 선언
    private final BoardRepository boardRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ValidationService validationService;
    private final ElasticSearchService elasticSearchService;
    private final ObjectMapper objectMapper;
    private final RabbitMQSender rabbitMQSender;
    private RedisTemplate<String, Object> redisTemplate;

    // 생성자 주입 방식으로 의존성 주입
    // @Qualifier 없이도 타입이 모두 다르므로 자동 주입 가능
    @Autowired
    public CommentService(BoardRepository boardRepository, ArticleRepository articleRepository,
                          UserRepository userRepository, CommentRepository commentRepository,
                          ValidationService validationService, ElasticSearchService elasticSearchService,
                          ObjectMapper objectMapper, RabbitMQSender rabbitMQSender,
                          RedisTemplate<String, Object> redisTemplate) {
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

    // 댓글 작성 메소드
    // @Transactional: 트랜잭션 처리를 위한 어노테이션
    @Transactional
    public Comment writeComment(Long boardId, Long articleId, WriteCommentDto dto) {
        // 현재 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 댓글 작성 가능 여부 확인 (시간 제한)
        if (!this.isCanWriteComment()) {
            throw new RateLimitException("comment not written by rate limit");
        }

        // 현재 사용자 정보 검증
        User author = validationService.currentUser(userRepository);

        // 게시판과 게시글 존재 여부 검증
        validationService.validate(boardId, boardRepository, "Board");
        Article article = validationService.validate(articleId, articleRepository, "Article");

        // 삭제된 게시글인지 확인
        if (article.getIsDeleted()) {
            throw new ForbiddenException("article is deleted");
        }

        // 새 댓글 생성 및 저장
        Comment comment = new Comment();
        comment.setArticle(article);
        comment.setAuthor(author);
        comment.setContent(dto.getContent());
        commentRepository.save(comment);

        // RabbitMQ를 통한 비동기 이벤트 발생
        WriteComment writeComment = new WriteComment();
        writeComment.setCommentId(comment.getId());
        rabbitMQSender.send(writeComment);
        return comment;
    }

    // 댓글 수정 메소드
    @Transactional
    public Comment editComment(Long boardId, Long articleId, Long commentId, WriteCommentDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 댓글 수정 가능 여부 확인 (시간 제한)
        if (!this.isCanEditComment()) {
            throw new RateLimitException("comment not written by rate limit");
        }

        // 사용자, 게시판, 게시글 검증
        User author = validationService.currentUser(userRepository);
        validationService.validate(boardId, boardRepository, "Board");
        Article article = validationService.validate(articleId, articleRepository, "Article");

        if (article.getIsDeleted()) {
            throw new ForbiddenException("article is deleted");
        }

        // 댓글 존재 여부 및 작성자 일치 여부 확인
        Comment comment = validationService.validate(commentId, commentRepository, "Comment");
        if (comment.getAuthor() != author) {
            throw new ForbiddenException("comment author different");
        }

        // 댓글 내용 수정
        if (dto.getContent() != null) {
            comment.setContent(dto.getContent());
        }

        commentRepository.save(comment);
        return comment;
    }

    // 댓글 삭제 메소드
    public boolean deleteComment(Long boardId, Long articleId, Long commentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 댓글 수정 가능 여부 확인
        if (!this.isCanEditComment()) {
            throw new RateLimitException("comment not written by rate limit");
        }

        // 사용자, 게시판, 게시글 검증
        User author = validationService.currentUser(userRepository);
        validationService.validate(boardId, boardRepository, "Board");
        Article article = validationService.validate(articleId, articleRepository, "Article");

        if (article.getIsDeleted()) {
            throw new ForbiddenException("article is deleted");
        }

        // 댓글 존재 여부 및 삭제 상태 확인
        Comment comment = validationService.validate(commentId, commentRepository, "Comment");
        if (comment.getIsDeleted()) {
            throw new ResourceNotFoundException("comment is deleted");
        }

        // 댓글 작성자 확인
        if (comment.getAuthor() != author) {
            throw new ForbiddenException("comment author different");
        }

        // 소프트 삭제 처리
        comment.setIsDeleted(true);
        commentRepository.save(comment);
        return true;
    }

    // 댓글 작성 가능 여부 확인 (1분 제한)
    private boolean isCanWriteComment() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 사용자의 최근 댓글 조회
        Comment latestComment = commentRepository.findLatestCommentOrderByCreatedDate(userDetails.getUsername());
        if (latestComment == null) {
            return true;
        }
        return this.isDifferenceMoreThanOneMinutes(latestComment.getCreatedDate());
    }

    // 댓글 수정 가능 여부 확인 (1분 제한)
    private boolean isCanEditComment() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Comment latestComment = commentRepository.findLatestCommentOrderByCreatedDate(userDetails.getUsername());
        if (latestComment == null || latestComment.getUpdatedDate() == null) {
            return true;
        }
        return this.isDifferenceMoreThanOneMinutes(latestComment.getUpdatedDate());
    }

    // 시간 차이가 1분 이상인지 확인하는 유틸리티 메소드
    private boolean isDifferenceMoreThanOneMinutes(LocalDateTime localDateTime) {
        LocalDateTime dateAsLocalDateTime = new Date().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        Duration duration = Duration.between(localDateTime, dateAsLocalDateTime);
        return Math.abs(duration.toMinutes()) > 1;
    }

    // 게시글 조회 메소드 (비동기)
    @Async
    @Transactional
    protected CompletableFuture<Article> getArticle(Long boardId, Long articleId) throws JsonProcessingException {
        // Redis에서 인기 게시글 조회
        Object yesterdayHotArticleTempObj = redisTemplate.opsForHash().get(DailyHotArticleTasks.YESTERDAY_REDIS_KEY + articleId, articleId);
        Object weekHotArticleTempObj = redisTemplate.opsForHash().get(DailyHotArticleTasks.WEEK_REDIS_KEY + articleId, articleId);

        // Redis에 캐시된 인기 게시글이 있는 경우
        if (yesterdayHotArticleTempObj != null || weekHotArticleTempObj != null) {
            HotArticle hotArticle = (HotArticle) (yesterdayHotArticleTempObj != null ? yesterdayHotArticleTempObj : weekHotArticleTempObj);
            // 캐시된 데이터로 Article 객체 생성
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

        // 게시판과 게시글 검증
        validationService.validate(boardId, boardRepository, "Board");
        Article article = validationService.validate(articleId, articleRepository, "Article");

        if (article.getIsDeleted()) {
            throw new ResourceNotFoundException("article not found");
        }

        // 조회수 증가 및 저장
        article.setViewCount(article.getViewCount() + 1);
        articleRepository.save(article);

        // ElasticSearch에 게시글 정보 인덱싱
        String articleJson = objectMapper.writeValueAsString(article);
        elasticSearchService.indexArticleDocument(article.getId().toString(), articleJson).block();
        return CompletableFuture.completedFuture(article);
    }

    // 댓글 목록 조회 메소드 (비동기)
    @Async
    protected CompletableFuture<List<Comment>> getComments(Long articleId) {
        return CompletableFuture.completedFuture(commentRepository.findByArticleId(articleId));
    }

    // 게시글과 댓글 목록을 함께 조회하는 메소드
    public CompletableFuture<Article> getArticleWithComment(Long boardId, Long articleId) throws JsonProcessingException {
        // 게시글과 댓글을 비동기로 조회
        CompletableFuture<Article> articleFuture = this.getArticle(boardId, articleId);
        CompletableFuture<List<Comment>> commentsFuture = this.getComments(articleId);

        // 두 비동기 작업이 모두 완료되면 결과를 합쳐서 반환
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
