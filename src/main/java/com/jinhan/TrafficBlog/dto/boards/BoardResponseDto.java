package com.jinhan.TrafficBlog.dto.boards;

import com.jinhan.TrafficBlog.entity.Board;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardResponseDto {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    public BoardResponseDto(Board board) {
        this.id = board.getId();
        this.title = board.getTitle();
        this.description = board.getDescription();
        this.createdDate = board.getCreatedDate();
        this.updatedDate = board.getUpdatedDate();
    }

}