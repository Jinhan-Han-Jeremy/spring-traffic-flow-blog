package com.jinhan.TrafficBlog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class ElasticSearchService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public ElasticSearchService(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Elasticsearch에서 주어진 키워드로 게시글을 검색하는 메서드
     * @param keyword 검색할 키워드
     * @return Mono<List<Long>> - 검색 결과로 나온 게시글 ID의 비동기 리스트
     */
    public Mono<List<Long>> articleSearch(String keyword) {
        // Elasticsearch에 요청할 검색 쿼리를 JSON 문자열로 구성
        String query = String.format("{\"_source\": false, \"query\": {\"match\": {\"content\": \"%s\"}}, \"fields\": [\"_id\"], \"size\": 10}", keyword);

        // WebClient를 사용해 Elasticsearch의 /article/_search 엔드포인트로 POST 요청 전송
        return webClient.post()
                .uri("/article/_search")
                .header("Content-Type", "application/json") // 요청 데이터 형식 설정
                .header("Accept", "application/json") // 응답 데이터 형식 설정
                .bodyValue(query)
                .retrieve() // 서버로부터의 응답 처리 시작
                .bodyToMono(String.class)// 응답 본문을 문자열로 변환
                .flatMap(this::extractIds); // 응답 문자열에서 ID 목록 추출
    }

    /**
     * Elasticsearch 응답에서 게시글 ID를 추출하는 메서드
     * @param responseBody Elasticsearch 응답 본문
     * @return Mono<List<Long>> - 추출된 게시글 ID의 비동기 리스트
     */
    private Mono<List<Long>> extractIds(String responseBody) {
        List<Long> ids = new ArrayList<>();
        try {
            // 응답 본문을 JSON으로 파싱하고 "hits.hits" 경로에 있는 데이터를 읽음
            JsonNode hits = objectMapper.readTree(responseBody).path("hits").path("hits");
            hits.forEach(hit -> ids.add(hit.path("_id").asLong()));
        }
        catch (IOException e) {
            return Mono.error(e);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return Mono.just(ids);
    }

    /**
     * Elasticsearch에 게시글 문서를 색인하는 메서드
     * @param id 게시글 ID
     * @param document 게시글 데이터 (JSON 형식)
     * @return Mono<String> - 색인 작업 결과를 나타내는 비동기 문자열
     */
    public Mono<String> indexArticleDocument(String id, String document) {
        return webClient.put()
                .uri("/article/_doc/{id}", id)  // 색인할 게시글 ID를 URL 경로에 포함
                .header("Content-Type", "application/json") // 요청 데이터 형식 설정
                .header("Accept", "application/json")
                .bodyValue(document)  // 요청 본문에 게시글 데이터 포함
                .retrieve() // 서버로부터의 응답 처리 시작
                .bodyToMono(String.class); // 응답 본문을 문자열로 변환
    }
}