package com.test.courseservice.controller;

import com.test.courseservice.dto.QuizCreateRequest;
import com.test.courseservice.dto.QuizResponse;
import com.test.courseservice.dto.QuizUpdateRequest;
import com.test.courseservice.security.AuthenticatedUser;
import com.test.courseservice.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/{courseId}/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping
    public ResponseEntity<QuizResponse> createQuiz(@PathVariable String courseId,
                                                   @Valid @RequestBody QuizCreateRequest request,
                                                   @AuthenticationPrincipal AuthenticatedUser currentUser) {
        QuizResponse response = quizService.createQuiz(courseId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<QuizResponse> getQuiz(@PathVariable String courseId,
                                                @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(quizService.getQuiz(courseId, currentUser));
    }

    @PutMapping
    public ResponseEntity<QuizResponse> updateQuiz(@PathVariable String courseId,
                                                   @Valid @RequestBody QuizUpdateRequest request,
                                                   @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(quizService.updateQuiz(courseId, request, currentUser));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteQuiz(@PathVariable String courseId,
                                           @AuthenticationPrincipal AuthenticatedUser currentUser) {
        quizService.deleteQuiz(courseId, currentUser);
        return ResponseEntity.noContent().build();
    }
}