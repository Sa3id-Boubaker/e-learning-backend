package com.test.courseservice.controller;

import com.test.courseservice.dto.QuestionCreateRequest;
import com.test.courseservice.dto.QuestionResponse;
import com.test.courseservice.dto.QuestionUpdateRequest;
import com.test.courseservice.security.AuthenticatedUser;
import com.test.courseservice.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes/{quizId}/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    public ResponseEntity<QuestionResponse> createQuestion(@PathVariable String quizId,
                                                           @Valid @RequestBody QuestionCreateRequest request,
                                                           @AuthenticationPrincipal AuthenticatedUser currentUser) {
        QuestionResponse response = questionService.createQuestion(quizId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<QuestionResponse>> getQuestions(@PathVariable String quizId,
                                                               @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(questionService.getQuestions(quizId, currentUser));
    }

    @PutMapping("/{questionId}")
    public ResponseEntity<QuestionResponse> updateQuestion(@PathVariable String quizId,
                                                           @PathVariable String questionId,
                                                           @Valid @RequestBody QuestionUpdateRequest request,
                                                           @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(questionService.updateQuestion(quizId, questionId, request, currentUser));
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable String quizId,
                                               @PathVariable String questionId,
                                               @AuthenticationPrincipal AuthenticatedUser currentUser) {
        questionService.deleteQuestion(quizId, questionId, currentUser);
        return ResponseEntity.noContent().build();
    }
}