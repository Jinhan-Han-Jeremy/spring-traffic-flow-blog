package com.jinhan.TrafficBlog.repository.jpa;

import com.jinhan.TrafficBlog.entity.AdClickStat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdClickStatRepository extends JpaRepository<AdClickStat, Long> {
}