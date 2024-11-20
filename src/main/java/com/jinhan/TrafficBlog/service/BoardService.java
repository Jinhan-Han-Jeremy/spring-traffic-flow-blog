package com.jinhan.TrafficBlog.service;

import com.jinhan.TrafficBlog.dto.boards.BoardDto;
import com.jinhan.TrafficBlog.dto.boards.BoardResponseDto;
import com.jinhan.TrafficBlog.entity.Board;
import com.jinhan.TrafficBlog.repository.jpa.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class BoardService {

    private final BoardRepository boardRepository;
    private final ValidationService validationService;

    @Autowired
    public BoardService(BoardRepository boardRepository, ValidationService validationService) {
        this.boardRepository = boardRepository;
        this.validationService = validationService;
    }

    // Create a new board
    @Transactional
    public BoardResponseDto createBoard(BoardDto boardDto) {

        boolean boardExists = boardRepository.existsByTitle(boardDto.getTitle());
        if (boardExists) {
            throw new IllegalArgumentException("A board with the same title already exists.");
        }

        Board board = new Board();
        board.setTitle(boardDto.getTitle());
        board.setDescription(boardDto.getDescription());
        boardRepository.save(board);
        return new BoardResponseDto(board);
    }

    // Update an existing board
    @Transactional
    public Optional<BoardResponseDto> updateBoard(Long boardId, BoardDto boardDto) {

        // ValidationService를 통해 Board 검증 및 조회
        Board board = validationService.validate(boardId, boardRepository, "Board");

        // 보드 정보를 수정하고 저장
        board.setTitle(boardDto.getTitle());
        board.setDescription(boardDto.getDescription());
        boardRepository.save(board);

        return Optional.of(new BoardResponseDto(board));
    }

    // Delete a board
    @Transactional
    public void deleteBoard(Long boardId) {
        boardRepository.deleteById(boardId);
    }

    // Find a board by ID (for retrieving single board)
    public Optional<BoardResponseDto> findBoardById(Long boardId) {
        RepositoryInterface<Board> repositoryInterface = (RepositoryInterface<Board>) boardRepository;
        return repositoryInterface.findById(boardId)
                .map(BoardResponseDto::new);
    }
}