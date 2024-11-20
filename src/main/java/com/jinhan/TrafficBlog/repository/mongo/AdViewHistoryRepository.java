package com.jinhan.TrafficBlog.repository.mongo;

import com.jinhan.TrafficBlog.entity.AdViewHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AdViewHistoryRepository extends MongoRepository<AdViewHistory, String> {
}