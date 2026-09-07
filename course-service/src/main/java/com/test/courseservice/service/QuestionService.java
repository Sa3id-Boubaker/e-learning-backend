package com.test.courseservice.service;

import com.test.courseservice.dto.OptionRequest;
import com.test.courseservice.dto.QuestionCreateRequest;
import com.test.courseservice.dto.QuestionResponse;
import com.test.courseservice.dto.QuestionUpdateRequest;
import com.test.courseservice.exception.InvalidQuestionException;
import com.test.courseservice.exception.QuestionNotFoundException;
import com.test.courseservice.model.Course;
import com.test.courseservice.model.Option;
import com.test.courseservice.model.Question;
import com.test.courseservice.model.Quiz;
import com.test.courseservice.repository.QuestionRepository;
import com.test.courseservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuizService quizService;

    public QuestionResponse createQuestion(String quizId, QuestionCreateRequest request, AuthenticatedUser currentUser) {

        Quiz quiz = quizService.resolveQuizById(quizId);
        Course course = quizService.resolveCourse(quiz.getCourseId());
        quizService.assertManageAccess(course, currentUser);

        validateOptions(request.getOptions());

        Question question = Question.builder()
                .quizId(quizId)
                .questionText(request.getQuestionText())
                .order(request.getOrder())
                .options(toOptions(request.getOptions()))
                .build();

        Question saved = questionRepository.save(question);
        return QuestionMapper.toResponse(saved, false);
    }

    public List<QuestionResponse> getQuestions(String quizId, AuthenticatedUser currentUser) {

        Quiz quiz = quizService.resolveQuizById(quizId);
        Course course = quizService.resolveCourse(quiz.getCourseId());
        quizService.assertViewAccess(course, currentUser);

        boolean isStudent = "ETUDIANT".equals(currentUser.role());

        return questionRepository.findByQuizIdOrderByOrderAsc(quizId).stream()
                .map(q -> QuestionMapper.toResponse(q, isStudent))
                .toList();
    }

    public QuestionResponse updateQuestion(String quizId, String questionId, QuestionUpdateRequest request, AuthenticatedUser currentUser) {

        Quiz quiz = quizService.resolveQuizById(quizId);
        Course course = quizService.resolveCourse(quiz.getCourseId());
        quizService.assertManageAccess(course, currentUser);

        Question question = resolveQuestion(questionId, quizId);

        if (request.getQuestionText() != null) question.setQuestionText(request.getQuestionText());
        if (request.getOrder() != null) question.setOrder(request.getOrder());
        if (request.getOptions() != null) {
            validateOptions(request.getOptions());
            question.setOptions(toOptions(request.getOptions()));
        }

        Question saved = questionRepository.save(question);
        return QuestionMapper.toResponse(saved, false);
    }

    public void deleteQuestion(String quizId, String questionId, AuthenticatedUser currentUser) {

        Quiz quiz = quizService.resolveQuizById(quizId);
        Course course = quizService.resolveCourse(quiz.getCourseId());
        quizService.assertManageAccess(course, currentUser);

        Question question = resolveQuestion(questionId, quizId);
        questionRepository.delete(question);
    }

    private Question resolveQuestion(String questionId, String quizId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException("Question not found with id: " + questionId));

        if (!question.getQuizId().equals(quizId)) {
            throw new QuestionNotFoundException("Question not found with id: " + questionId);
        }

        return question;
    }

    private void validateOptions(List<OptionRequest> options) {
        if (options == null || options.size() < 2) {
            throw new InvalidQuestionException("Each question must have at least 2 options");
        }
        boolean hasCorrect = options.stream().anyMatch(o -> Boolean.TRUE.equals(o.getCorrect()));
        if (!hasCorrect) {
            throw new InvalidQuestionException("At least one option must be marked as correct");
        }
    }

    private List<Option> toOptions(List<OptionRequest> requests) {
        return requests.stream()
                .map(r -> Option.builder()
                        .id(UUID.randomUUID().toString())
                        .text(r.getText())
                        .correct(r.getCorrect())
                        .build())
                .toList();
    }
}