package com.jinhan.TrafficBlog.controller;

import com.jinhan.TrafficBlog.dto.boards.BoardDto;
import com.jinhan.TrafficBlog.dto.boards.BoardResponseDto;
import com.jinhan.TrafficBlog.service.BoardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/boards")
public class BoardController {

    private static final Logger logger = LoggerFactory.getLogger(BoardController.class);


    private final BoardService boardService;

    @Autowired
    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    // Create a new board
    @PostMapping
    public ResponseEntity<BoardResponseDto> createBoard(@RequestBody BoardDto boardDto) {
        logger.info("Sample createBoard log");

        BoardResponseDto createdBoard = boardService.createBoard(boardDto);
        return ResponseEntity.ok(createdBoard);
    }

    // Update an existing board
    @PutMapping("/{boardId}")
    public ResponseEntity<BoardResponseDto> updateBoard(@PathVariable Long boardId, @RequestBody BoardDto boardDto) {
        Optional<BoardResponseDto> updatedBoard = boardService.updateBoard(boardId, boardDto);
        return updatedBoard.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Delete a board
    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long boardId) {
        boardService.deleteBoard(boardId);
        return ResponseEntity.noContent().build();
    }
}