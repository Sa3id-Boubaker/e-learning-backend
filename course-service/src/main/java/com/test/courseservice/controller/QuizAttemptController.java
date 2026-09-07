package com.test.courseservice.controller;

import com.test.courseservice.dto.QuizResultResponse;
import com.test.courseservice.dto.QuizSubmitRequest;
import com.test.courseservice.security.AuthenticatedUser;
import com.test.courseservice.service.QuizAttemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes/{quizId}")
@RequiredArgsConstructor
public class QuizAttemptController {

    private final QuizAttemptService quizAttemptService;

    @PostMapping("/submit")
    public ResponseEntity<QuizResultResponse> submitQuiz(@PathVariable String quizId,
                                                         @Valid @RequestBody QuizSubmitRequest request,
                                                         @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(quizAttemptService.submitQuiz(quizId, request, currentUser));
    }

    @GetMapping("/my-result")
    public ResponseEntity<QuizResultResponse> getMyResult(@PathVariable String quizId,
                                                          @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(quizAttemptService.getMyResult(quizId, currentUser));
    }

    @GetMapping("/my-attempts")
    public ResponseEntity<List<QuizResultResponse>> getMyAttempts(@PathVariable String quizId,
                                                                  @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(quizAttemptService.getMyAttempts(quizId, currentUser));
    }
}