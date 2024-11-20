package com.jinhan.TrafficBlog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

@Configuration
public class WebClientConfig {
    // Spring 애플리케이션 컨텍스트에 WebClient Bean을 등록
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                // 기본 요청 URL 설정 (예: Elasticsearch API 서버)
                .baseUrl("http://localhost:9200")
                // 모든 요청에 기본 인증 헤더 추가 (HTTP Basic Auth 사용)
                .defaultHeaders(headers -> headers.setBasicAuth("jinhan", "57575han"))
                // 요청 및 응답 처리 전략 설정
                .exchangeStrategies(ExchangeStrategies.builder()
                        // 디코딩 시 사용할 메모리 크기 제한 설정 (16MB)
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(16 * 1024 * 1024))
                        .build())
                .build(); // WebClient 인스턴스 생성 및 반환
    }
}