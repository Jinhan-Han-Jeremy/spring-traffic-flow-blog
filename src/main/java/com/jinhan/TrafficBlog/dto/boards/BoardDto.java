package com.jinhan.TrafficBlog.dto.boards;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class BoardDto {
    @NotEmpty(message = "Title is required")
    private String title;

    @NotEmpty(message = "Description is required")
    private String description;
}