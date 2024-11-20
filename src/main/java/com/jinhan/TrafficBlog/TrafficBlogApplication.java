package com.jinhan.TrafficBlog;

import com.jinhan.TrafficBlog.repository.mongo.AdClickHistoryRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableJpaRepositories(
		basePackages = "com.jinhan.TrafficBlog.repository.jpa",
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AdClickHistoryRepository.class)
)
@EnableMongoRepositories(basePackages = "com.jinhan.TrafficBlog.repository.mongo")
public class TrafficBlogApplication {
	public static void main(String[] args) {
		SpringApplication.run(TrafficBlogApplication.class, args);
	}
}