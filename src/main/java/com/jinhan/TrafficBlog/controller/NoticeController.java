package com.jinhan.TrafficBlog.controller;

import com.jinhan.TrafficBlog.dto.WriteNotice;
import com.jinhan.TrafficBlog.entity.Notice;
import com.jinhan.TrafficBlog.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {
    private final NoticeService noticeService;

    @Autowired
    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @PostMapping("")
    public ResponseEntity<Notice> addNotice(@RequestBody WriteNotice dto) {
        return ResponseEntity.ok(noticeService.writeNotice(dto));
    }

    @GetMapping("/{noticeId}")
    public ResponseEntity<Notice> getNotice(@PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeService.getNotice(noticeId));
    }
}