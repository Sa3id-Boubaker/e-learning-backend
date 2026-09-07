package com.test.courseservice.service;

import com.test.courseservice.dto.*;
import com.test.courseservice.exception.*;
import com.test.courseservice.model.Course;
import com.test.courseservice.model.Question;
import com.test.courseservice.model.Quiz;
import com.test.courseservice.repository.CourseRepository;
import com.test.courseservice.repository.QuestionRepository;
import com.test.courseservice.repository.QuizRepository;
import com.test.courseservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentService enrollmentService;

    public QuizResponse createQuiz(String courseId, QuizCreateRequest request, AuthenticatedUser currentUser) {

        Course course = resolveCourse(courseId);
        assertManageAccess(course, currentUser);

        if (quizRepository.existsByCourseId(courseId)) {
            throw new QuizAlreadyExistsException("A quiz already exists for this course");
        }

        LocalDateTime now = LocalDateTime.now();

        Quiz quiz = Quiz.builder()
                .courseId(courseId)
                .title(request.getTitle())
                .description(request.getDescription())
                .passingScore(request.getPassingScore() != null ? request.getPassingScore() : 70)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Quiz saved = quizRepository.save(quiz);
        return toResponse(saved, currentUser, List.of());
    }

    public QuizResponse getQuiz(String courseId, AuthenticatedUser currentUser) {

        Course course = resolveCourse(courseId);
        assertViewAccess(course, currentUser);

        Quiz quiz = resolveQuizByCourse(courseId);
        List<Question> questions = questionRepository.findByQuizIdOrderByOrderAsc(quiz.getId());

        return toResponse(quiz, currentUser, questions);
    }

    public QuizResponse updateQuiz(String courseId, QuizUpdateRequest request, AuthenticatedUser currentUser) {

        Course course = resolveCourse(courseId);
        assertManageAccess(course, currentUser);

        Quiz quiz = resolveQuizByCourse(courseId);

        if (request.getTitle() != null) quiz.setTitle(request.getTitle());
        if (request.getDescription() != null) quiz.setDescription(request.getDescription());
        if (request.getPassingScore() != null) quiz.setPassingScore(request.getPassingScore());

        quiz.setUpdatedAt(LocalDateTime.now());

        Quiz saved = quizRepository.save(quiz);
        List<Question> questions = questionRepository.findByQuizIdOrderByOrderAsc(saved.getId());

        return toResponse(saved, currentUser, questions);
    }

    public void deleteQuiz(String courseId, AuthenticatedUser currentUser) {

        Course course = resolveCourse(courseId);
        assertManageAccess(course, currentUser);

        Quiz quiz = resolveQuizByCourse(courseId);

        questionRepository.deleteByQuizId(quiz.getId());
        quizRepository.delete(quiz);
    }

    Course resolveCourse(String courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
    }

    Quiz resolveQuizByCourse(String courseId) {
        return quizRepository.findByCourseId(courseId)
                .orElseThrow(() -> new QuizNotFoundException("No quiz found for course with id: " + courseId));
    }

    Quiz resolveQuizById(String quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found with id: " + quizId));
    }

    void assertViewAccess(Course course, AuthenticatedUser currentUser) {
        switch (currentUser.role()) {
            case "ADMIN" -> { }
            case "FORMATEUR" -> {
                if (!course.getInstructorId().equals(currentUser.userId())) {
                    throw new CourseAccessDeniedException("This course does not belong to you");
                }
            }
            default -> {
                if (!Boolean.TRUE.equals(course.getPublished())) {
                    throw new CourseAccessDeniedException("This course is not available");
                }
                enrollmentService.assertActiveEnrollment(currentUser.userId(), course.getId());
            }
        }
    }

    void assertManageAccess(Course course, AuthenticatedUser currentUser) {
        boolean isAdmin = "ADMIN".equals(currentUser.role());
        boolean isOwner = course.getInstructorId().equals(currentUser.userId());

        if (!isAdmin && !isOwner) {
            throw new NotCourseOwnerException("You are not the owner of this course");
        }
    }

    private QuizResponse toResponse(Quiz quiz, AuthenticatedUser currentUser, List<Question> questions) {
        boolean isStudent = "ETUDIANT".equals(currentUser.role());

        List<QuestionResponse> questionResponses = questions.stream()
                .map(q -> QuestionMapper.toResponse(q, isStudent))
                .toList();

        return QuizResponse.builder()
                .id(quiz.getId())
                .courseId(quiz.getCourseId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .passingScore(quiz.getPassingScore())
                .questions(questionResponses)
                .createdAt(quiz.getCreatedAt())
                .updatedAt(quiz.getUpdatedAt())
                .build();
    }

    Optional<Quiz> findQuizByCourseOptional(String courseId) {
        return quizRepository.findByCourseId(courseId);
    }
}