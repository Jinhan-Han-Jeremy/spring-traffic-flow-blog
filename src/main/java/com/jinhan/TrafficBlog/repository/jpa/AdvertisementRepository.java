package com.jinhan.TrafficBlog.repository.jpa;

import com.jinhan.TrafficBlog.entity.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvertisementRepository  extends JpaRepository<Advertisement, Long> {

}