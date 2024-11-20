package com.jinhan.TrafficBlog.repository.mongo;

import com.jinhan.TrafficBlog.entity.AdClickHistory;
import com.jinhan.TrafficBlog.entity.AdViewHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AdClickHistoryRepository extends MongoRepository<AdClickHistory, String> {

}