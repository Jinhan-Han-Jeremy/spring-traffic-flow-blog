package com.jinhan.TrafficBlog.repository;

import com.jinhan.TrafficBlog.entity.AdClickHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AdClickHistoryRepository extends MongoRepository<AdClickHistory, String> {
}