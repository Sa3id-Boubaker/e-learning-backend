package com.test.courseservice.service;

import com.test.courseservice.dto.OptionResponse;
import com.test.courseservice.dto.QuestionResponse;
import com.test.courseservice.model.Question;

import java.util.List;

final class QuestionMapper {

    private QuestionMapper() {}

    static QuestionResponse toResponse(Question question, boolean hideCorrectAnswer) {
        List<OptionResponse> options = question.getOptions().stream()
                .map(o -> OptionResponse.builder()
                        .id(o.getId())
                        .text(o.getText())
                        .correct(hideCorrectAnswer ? null : o.getCorrect())
                        .build())
                .toList();

        return QuestionResponse.builder()
                .id(question.getId())
                .quizId(question.getQuizId())
                .questionText(question.getQuestionText())
                .order(question.getOrder())
                .options(options)
                .build();
    }
}