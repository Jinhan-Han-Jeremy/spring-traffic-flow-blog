package com.jinhan.TrafficBlog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TrafficBlogApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrafficBlogApplication.class, args);
	}

}