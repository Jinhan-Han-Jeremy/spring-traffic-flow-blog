package com.jinhan.TrafficBlog.exception;


public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}