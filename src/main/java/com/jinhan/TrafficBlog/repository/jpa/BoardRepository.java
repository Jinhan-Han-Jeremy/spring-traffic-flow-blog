package com.jinhan.TrafficBlog.repository.jpa;

import com.jinhan.TrafficBlog.entity.Board;
import com.jinhan.TrafficBlog.service.RepositoryInterface;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long>, RepositoryInterface<Board> {
    boolean existsByTitle(String title);
}