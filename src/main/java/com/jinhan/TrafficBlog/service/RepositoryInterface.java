package com.jinhan.TrafficBlog.service;

import java.util.Optional;

public interface RepositoryInterface<T> {
    Optional<T> findById(Long id);
}