package com.jinhan.TrafficBlog.service;

import com.jinhan.TrafficBlog.dto.EditArticleDto;
import com.jinhan.TrafficBlog.dto.WriteArticleDto;
import com.jinhan.TrafficBlog.entity.Article;
import com.jinhan.TrafficBlog.pojo.WriteArticle;
import com.jinhan.TrafficBlog.entity.Board;
import com.jinhan.TrafficBlog.entity.User;
import com.jinhan.TrafficBlog.exception.ForbiddenException;
import com.jinhan.TrafficBlog.exception.RateLimitException;
import com.jinhan.TrafficBlog.repository.jpa.ArticleRepository;
import com.jinhan.TrafficBlog.repository.jpa.BoardRepository;
import com.jinhan.TrafficBlog.repository.jpa.UserRepository;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class ArticleService {
    private final BoardRepository boardRepository;
    private final ArticleRepository articleRepository;

    private final UserRepository userRepository;

    private final ElasticSearchService elasticSearchService;

    private final ObjectMapper objectMapper;

    private final RabbitMQSender rabbitMQSender;

    private final ValidationService validationService;

    // 생성자를 통해 의존성 주입
    @Autowired
    public ArticleService(BoardRepository boardRepository, ArticleRepository articleRepository, UserRepository userRepository,
                          ElasticSearchService elasticSearchService, ObjectMapper objectMapper, RabbitMQSender rabbitMQSender, ValidationService validationService) {
        this.boardRepository = boardRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.elasticSearchService = elasticSearchService;
        this.objectMapper = objectMapper;
        this.rabbitMQSender = rabbitMQSender;
        this.validationService = validationService;
    }

    //게시글 수정 제한 확인 로직
    private void validateEditRateLimit() {
        if (!this.isCanEditArticle()) {
            throw new RateLimitException("Article edit rate limit exceeded");
        }
    }

    // 게시글, 사용자 검증 및 게시글 작성자 확인 로직
    private Article articleValidateAndRetrieve(Long articleId, boolean checkAuthorValidation) {
        // 게시글 조회 및 검증
        // 게시글 정보가 존재하지 않는 경우 예외 발생
        Article article = validationService.validate(articleId, articleRepository, "article");

        if (checkAuthorValidation) {
            // 현재 인증된 사용자 조회
            User author = validationService.currentUser(userRepository);

            if (!article.getAuthor().equals(author)) {
                throw new ForbiddenException("Article author mismatch");
            }

            // 게시글 수정 제한 확인
            validateEditRateLimit();
        }

        return article;
    }

    //@Transactional을 활용해 데이터 정합성을 보장
    @Transactional
    public Article writeArticle(Long boardId, WriteArticleDto dto) throws JsonProcessingException {

        User author = validationService.currentUser(userRepository);

        // 게시글 작성 제한을 초과했는지 확인하는 메서드 호출
        if (!this.isCanWriteArticle()) {
            // 작성 제한이 초과되면 예외 발생
            throw new RateLimitException("article not written by rate limit");
        }

        // 게시판 정보 조회 (boardId 기준으로 Board 엔티티 조회)
        Board board = validationService.validate(boardId, boardRepository, "Board");

        // 새로운 Article 객체 생성 및 게시판, 작성자, 제목, 내용을 설정
        Article article = new Article();
        article.setBoard(board);       // 조회된 게시판 정보 설정
        article.setAuthor(author);     // 조회된 작성자 정보 설정
        article.setTitle(dto.getTitle());    // DTO에서 전달받은 제목 설정
        article.setContent(dto.getContent()); // DTO에서 전달받은 내용 설정
        articleRepository.save(article);

        this.indexArticle(article);
        WriteArticle articleNotification = new WriteArticle();
        articleNotification.setArticleId(article.getId());
        articleNotification.setUserId(author.getId());

        //rabbitMQ의 메세지 전송 및 느슨한 결합 제공
        rabbitMQSender.send(articleNotification);

        return article;
    }

    public List<Article> firstGetArticle(Long boardId) {
        return articleRepository.findTop10ByBoardIdOrderByCreatedDateDesc(boardId);
    }

    public List<Article> getOldArticle(Long boardId, Long articleId) {
        return articleRepository.findTop10ByBoardIdAndArticleIdLessThanOrderByCreatedDateDesc(boardId, articleId);
    }

    public List<Article> getNewArticle(Long boardId, Long articleId) {
        return articleRepository.findTop10ByBoardIdAndArticleIdGreaterThanOrderByCreatedDateDesc(boardId, articleId);
    }

    @Transactional
    public Article editArticle(Long boardId, Long articleId, EditArticleDto dto) throws JsonProcessingException {
        // 공통 검증 로직 호출
        // 게시판 정보 조회 (boardId 기준으로 Board 엔티티 조회)
        validationService.validate(boardId,  boardRepository, "Board");

        Article article = articleValidateAndRetrieve(articleId, true);
        // true: 수정 가능한 사용자 검증

        // DTO의 값으로 제목과 내용을 설정
        dto.getTitle().ifPresent(article::setTitle);
        dto.getContent().ifPresent(article::setContent);

        // 저장 및 색인 처리
        articleRepository.save(article); // article 자체는 이미 존재하는 엔티티이므로 Optional에서 벗어남
        this.indexArticle(article);

        return article;
    }

    @Transactional
    public boolean deleteArticle(Long boardId, Long articleId) throws JsonProcessingException {
        // 공통 검증 로직 호출
        UserDetails userDetails = validationService.currentUser(userRepository);;
        userRepository.findByUsername(userDetails.getUsername());

        // 게시판 정보 조회 (boardId 기준으로 Board 엔티티 조회)
        validationService.validate(boardId, boardRepository, "Board");

        Article article = articleValidateAndRetrieve(articleId, true);

        // true: 수정 가능한 사용자 검증
        article.setIsDeleted(true);

        //articleRepository로 데이터를 저장 및 Dirty Checking에 의해 밣생하는 문제가 예방
        articleRepository.save(article);

        this.indexArticle(article);
        return true;
    }

    // Rate Limit 검사
    private boolean isCanWriteArticle() {
        // 공통 검증 로직 호출
        UserDetails userDetails = validationService.currentUser(userRepository);;

        Article latestArticle = articleRepository.findLatestArticleByAuthorUsernameOrderByCreatedDate(userDetails.getUsername());
        if (latestArticle == null) {
            return true;
        }
        return this.isDifferenceMoreThanFiveMinutes(latestArticle.getCreatedDate());
    }

    // 사용자가 게시글을 수정할 수 있는지 확인하는 메서드
    private boolean isCanEditArticle() {
        // 현재 인증된 사용자의 정보를 가져옴
        UserDetails userDetails = validationService.currentUser(userRepository);;

        // 가장 최근에 수정된 게시글 조회
        Article latestArticle = articleRepository.findLatestArticleByAuthorUsernameOrderByUpdatedDate(userDetails.getUsername());

        if (latestArticle == null || latestArticle.getUpdatedDate() == null) {
            return true;
        }

        return this.isDifferenceMoreThanFiveMinutes(latestArticle.getUpdatedDate());
    }

    //시간 변수들의 차이가 5분 이상인지 확인하는 메서드
    private boolean isDifferenceMoreThanFiveMinutes(LocalDateTime localDateTime) {

        // 두 시간의 차이를 계산
        Duration duration = validationService.isDifferenceBetweenTimes(localDateTime);

        return Math.abs(duration.toMinutes()) > 5;
    }

    // Elasticsearch에 게시글을 색인하는 메서드
    public String indexArticle(Article article) throws JsonProcessingException {
        // Article 객체를 JSON 문자열로 변환
        String articleJson = objectMapper.writeValueAsString(article);

        // Elasticsearch 서비스에 게시글 색인 요청
        return elasticSearchService.indexArticleDocument(article.getId().toString(), articleJson).block();
    }

    // 키워드로 게시글을 검색하는 메서드
    public List<Article> searchArticle(String keyword) {
        // Elasticsearch를 통해 게시글 ID 목록을 비동기로 검색
        Mono<List<Long>> articleIds = elasticSearchService.articleSearch(keyword);
        try {
            // 검색된 게시글 ID 목록으로 실제 게시글 목록 조회
            return articleRepository.findAllById(articleIds.toFuture().get());
        } catch (InterruptedException e) {
            // 스레드가 중단되었을 경우 예외 발생
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            // 비동기 작업 중 예외 발생
            throw new RuntimeException(e);
        }
    }
}