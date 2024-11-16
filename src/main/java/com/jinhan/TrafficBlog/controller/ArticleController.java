package com.jinhan.TrafficBlog.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jinhan.TrafficBlog.dto.EditArticleDto;
import com.jinhan.TrafficBlog.dto.WriteArticleDto;
import com.jinhan.TrafficBlog.entity.Article;
import com.jinhan.TrafficBlog.service.ArticleService;
import com.jinhan.TrafficBlog.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("/api/boards")
public class ArticleController {
    private final AuthenticationManager authenticationManager;
    private final ArticleService articleService;

    private final CommentService commentService;

    // 생성자를 통해 필요한 서비스 주입, 의존성 주입을 위한 @Autowired 사용
    @Autowired
    public ArticleController(AuthenticationManager authenticationManager, ArticleService articleService, CommentService commentService) {
        this.authenticationManager = authenticationManager;
        this.articleService = articleService;
        this.commentService = commentService;
    }

    // 새로운 게시글을 작성하는 엔드포인트
    @PostMapping("/{boardId}/articles")
    public ResponseEntity<Article> writeArticle(@PathVariable Long boardId,
                                                @RequestBody WriteArticleDto writeArticleDto) throws JsonProcessingException {
        return ResponseEntity.ok(articleService.writeArticle(boardId, writeArticleDto));
    }

    // 특정 게시판(boardId)에 속한 게시글을 조회하는 엔드포인트
    // lastId, firstId 파라미터를 통해 이전/최신 게시글을 가져올 수 있음
    @GetMapping("/{boardId}/articles")
    public ResponseEntity<List<Article>> getArticle(@PathVariable Long boardId,
                                                    @RequestParam(required = false) Long lastId,
                                                    @RequestParam(required = false) Long firstId) {
        if (lastId != null) {
            return ResponseEntity.ok(articleService.getOldArticle(boardId, lastId));
        }
        if (firstId != null) {
            return ResponseEntity.ok(articleService.getNewArticle(boardId, firstId));
        }
        return ResponseEntity.ok(articleService.firstGetArticle(boardId));
    }

    // 키워드를 사용하여 게시글을 검색하는 엔드포인트
    @GetMapping("/{boardId}/articles/search")
    public ResponseEntity<List<Article>> searchArticle(@PathVariable Long boardId,
                                                       @RequestParam(required = true) String keyword) {
        if (keyword != null) {
            return ResponseEntity.ok(articleService.searchArticle(keyword));
        }
        return ResponseEntity.ok(articleService.firstGetArticle(boardId));
    }
    // 특정 게시글을 수정하는 엔드포인트
    @PutMapping("/{boardId}/articles/{articleId}")
    public ResponseEntity<Article> editArticle(@PathVariable Long boardId, @PathVariable Long articleId,
                                               @RequestBody EditArticleDto editArticleDto) throws JsonProcessingException {
        return ResponseEntity.ok(articleService.editArticle(boardId, articleId, editArticleDto));
    }
    // 특정 게시글을 삭제하는 엔드포인트
    @DeleteMapping("/{boardId}/articles/{articleId}")
    public ResponseEntity<String> deleteArticle(@PathVariable Long boardId, @PathVariable Long articleId) throws JsonProcessingException {
        articleService.deleteArticle(boardId, articleId);
        return ResponseEntity.ok("article is deleted");
    }
    // 특정 게시글과 댓글을 조회하는 엔드포인트, 비동기로 댓글을 가져옴
    @GetMapping("/{boardId}/articles/{articleId}")
    public ResponseEntity<Article> getArticleWithComment(@PathVariable Long boardId, @PathVariable Long articleId) throws JsonProcessingException {
        CompletableFuture<Article> article = commentService.getArticleWithComment(boardId, articleId);
        return ResponseEntity.ok(article.join());
    }
}