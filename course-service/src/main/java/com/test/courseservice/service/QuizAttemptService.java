package com.test.courseservice.service;

import com.test.courseservice.dto.*;
import com.test.courseservice.exception.CourseNotCompletedException;
import com.test.courseservice.exception.InvalidQuizSubmissionException;
import com.test.courseservice.exception.QuizAlreadyPassedException;
import com.test.courseservice.model.Course;
import com.test.courseservice.model.Question;
import com.test.courseservice.model.QuizAnswer;
import com.test.courseservice.model.QuizAttempt;
import com.test.courseservice.model.Quiz;
import com.test.courseservice.repository.QuestionRepository;
import com.test.courseservice.repository.QuizAttemptRepository;
import com.test.courseservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    private final QuizService quizService;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final StudentProgressService studentProgressService;

    public QuizResultResponse submitQuiz(String quizId, QuizSubmitRequest request, AuthenticatedUser currentUser) {

        Quiz quiz = quizService.resolveQuizById(quizId);
        Course course = quizService.resolveCourse(quiz.getCourseId());
        quizService.assertViewAccess(course, currentUser);

        if (findPassingAttempt(quizId, currentUser.userId()).isPresent()) {
            throw new QuizAlreadyPassedException("You have already passed this quiz. Retaking is not allowed.");
        }

        assertCourseCompleted(course.getId(), currentUser);

        List<Question> questions = questionRepository.findByQuizIdOrderByOrderAsc(quizId);
        Map<String, Question> questionsById = new LinkedHashMap<>();
        questions.forEach(q -> questionsById.put(q.getId(), q));

        Map<String, String> submittedAnswers = new LinkedHashMap<>();
        for (QuizAnswerRequest answer : request.getAnswers()) {
            Question question = questionsById.get(answer.getQuestionId());
            if (question == null) {
                throw new InvalidQuizSubmissionException("Question does not belong to this quiz: " + answer.getQuestionId());
            }
            boolean optionBelongs = question.getOptions().stream()
                    .anyMatch(o -> o.getId().equals(answer.getSelectedOptionId()));
            if (!optionBelongs) {
                throw new InvalidQuizSubmissionException("Selected option does not belong to question: " + answer.getQuestionId());
            }
            submittedAnswers.put(answer.getQuestionId(), answer.getSelectedOptionId());
        }

        int totalQuestions = questions.size();
        long correctCount = questions.stream()
                .filter(q -> {
                    String selectedOptionId = submittedAnswers.get(q.getId());
                    if (selectedOptionId == null) return false;
                    return q.getOptions().stream()
                            .anyMatch(o -> o.getId().equals(selectedOptionId) && Boolean.TRUE.equals(o.getCorrect()));
                })
                .count();

        int score = totalQuestions == 0 ? 0 : (int) Math.round((correctCount * 100.0) / totalQuestions);
        boolean passed = score >= quiz.getPassingScore();

        List<QuizAnswer> storedAnswers = request.getAnswers().stream()
                .map(a -> QuizAnswer.builder()
                        .questionId(a.getQuestionId())
                        .selectedOptionId(a.getSelectedOptionId())
                        .build())
                .toList();

        QuizAttempt attempt = QuizAttempt.builder()
                .quizId(quizId)
                .courseId(course.getId())
                .studentId(currentUser.userId())
                .answers(storedAnswers)
                .score(score)
                .passed(passed)
                .submittedAt(LocalDateTime.now())
                .build();

        QuizAttempt saved = quizAttemptRepository.save(attempt);

        return toResultResponse(saved, quiz.getPassingScore());
    }

    public QuizResultResponse getMyResult(String quizId, AuthenticatedUser currentUser) {

        Quiz quiz = quizService.resolveQuizById(quizId);

        QuizAttempt latest = quizAttemptRepository
                .findFirstByQuizIdAndStudentIdOrderBySubmittedAtDesc(quizId, currentUser.userId())
                .orElseThrow(() -> new com.test.courseservice.exception.QuizNotFoundException("No attempt found for this quiz"));

        return toResultResponse(latest, quiz.getPassingScore());
    }

    public List<QuizResultResponse> getMyAttempts(String quizId, AuthenticatedUser currentUser) {

        Quiz quiz = quizService.resolveQuizById(quizId);

        return quizAttemptRepository
                .findByQuizIdAndStudentIdOrderBySubmittedAtDesc(quizId, currentUser.userId())
                .stream()
                .map(a -> toResultResponse(a, quiz.getPassingScore()))
                .toList();
    }

    private void assertCourseCompleted(String courseId, AuthenticatedUser currentUser) {
        StudentProgressResponse progress = studentProgressService.getCourseProgress(courseId, currentUser);
        if (!Boolean.TRUE.equals(progress.getCompleted())) {
            throw new CourseNotCompletedException("You must complete all course videos before taking the quiz.");
        }
    }

    private QuizResultResponse toResultResponse(QuizAttempt attempt, Integer passingScore) {
        return QuizResultResponse.builder()
                .quizId(attempt.getQuizId())
                .courseId(attempt.getCourseId())
                .score(attempt.getScore())
                .passingScore(passingScore)
                .passed(attempt.getPassed())
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }

    Optional<QuizAttempt> findPassingAttempt(String quizId, String studentId) {
        return quizAttemptRepository.findByQuizIdAndStudentIdOrderBySubmittedAtDesc(quizId, studentId)
                .stream()
                .filter(a -> Boolean.TRUE.equals(a.getPassed()))
                .findFirst();
    }
}