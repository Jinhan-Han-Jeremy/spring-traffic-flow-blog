package com.jinhan.TrafficBlog.service;

import com.jinhan.TrafficBlog.dto.boards.BoardDto;
import com.jinhan.TrafficBlog.dto.boards.BoardResponseDto;
import com.jinhan.TrafficBlog.entity.Board;
import com.jinhan.TrafficBlog.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class BoardService {

    private final BoardRepository boardRepository;

    @Autowired
    public BoardService(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    // Create a new board
    @Transactional
    public BoardResponseDto createBoard(BoardDto boardDto) {
        Board board = new Board();
        board.setTitle(boardDto.getTitle());
        board.setDescription(boardDto.getDescription());
        boardRepository.save(board);
        return new BoardResponseDto(board);
    }

    // Update an existing board
    @Transactional
    public Optional<BoardResponseDto> updateBoard(Long boardId, BoardDto boardDto) {
        return boardRepository.findById(boardId).map(board -> {
            board.setTitle(boardDto.getTitle());
            board.setDescription(boardDto.getDescription());
            boardRepository.save(board);
            return new BoardResponseDto(board);
        });
    }

    // Delete a board
    @Transactional
    public void deleteBoard(Long boardId) {
        boardRepository.deleteById(boardId);
    }

    // Find a board by ID (for retrieving single board)
    public Optional<BoardResponseDto> findBoardById(Long boardId) {
        return boardRepository.findById(boardId).map(BoardResponseDto::new);
    }
}