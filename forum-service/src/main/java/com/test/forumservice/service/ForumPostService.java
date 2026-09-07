package com.test.forumservice.service;

import com.test.forumservice.client.CourseServiceClient;
import com.test.forumservice.client.TrainingServiceClient;
import com.test.forumservice.client.UserServiceClient;
import com.test.forumservice.dto.*;
import com.test.forumservice.entity.ForumPost;
import com.test.forumservice.entity.ForumPostType;
import com.test.forumservice.event.ForumEventPublisher;
import com.test.forumservice.exception.ForumAccessDeniedException;
import com.test.forumservice.exception.ForumPostNotFoundException;
import com.test.forumservice.exception.InvalidForumReferenceException;
import com.test.forumservice.mapper.ForumPostMapper;
import com.test.forumservice.repository.ForumBookmarkRepository;
import com.test.forumservice.repository.ForumCommentRepository;
import com.test.forumservice.repository.ForumPostRepository;
import com.test.forumservice.repository.ForumUpvoteRepository;
import com.test.forumservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForumPostService {

    private final ForumPostRepository postRepository;
    private final ForumCommentRepository commentRepository;
    private final ForumBookmarkRepository bookmarkRepository;
    private final ForumUpvoteRepository upvoteRepository;
    private final MongoTemplate mongoTemplate;
    private final ForumPostMapper forumPostMapper;
    private final UserServiceClient userServiceClient;
    private final CourseServiceClient courseServiceClient;
    private final TrainingServiceClient trainingServiceClient;
    private final ForumEventPublisher forumEventPublisher;

    // ============================== CREATE ==============================

    public ForumPostResponse createPost(ForumPostCreateRequest request, AuthenticatedUser user) {
        ResolvedRefs refs = resolveAndValidateReferences(request, user.token());

        LocalDateTime now = LocalDateTime.now();
        ForumPost post = ForumPost.builder()
                .authorId(user.userId())
                .title(request.getTitle())
                .body(request.getBody())
                .type(request.getType())
                .courseId(refs.courseId())
                .chapterId(refs.chapterId())
                .videoId(refs.videoId())
                .trainingId(refs.trainingId())
                .liveSessionId(refs.liveSessionId())
                .recordingId(refs.recordingId())
                .commentCount(0)
                .upvoteCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        post = postRepository.save(post);

        if (post.getType() == ForumPostType.TRAINING) {
            notifyAssignedInstructor(post, user);
        }

        return buildResponse(post, user);
    }

    /**
     * Notification is strictly best-effort: the post above is already durably saved by the time
     * this runs, so nothing here — a RabbitMQ outage, a TRAINING-SERVICE outage, or any other
     * unexpected failure — is allowed to propagate and turn a successful post creation into an
     * error response. Only fires for an ÉTUDIANT author (a FORMATEUR/ADMIN posting in their own
     * training's forum has no one "above" them to notify) and only when TRAINING-SERVICE actually
     * resolves a real instructorId.
     */
    private void notifyAssignedInstructor(ForumPost post, AuthenticatedUser user) {
        if (!"ETUDIANT".equals(user.role())) {
            return;
        }
        try {
            String instructorId = trainingServiceClient.getTrainingInstructorId(post.getTrainingId(), user.token());
            if (instructorId == null) {
                log.warn("Skipping ForumTrainingPostCreatedEvent — could not resolve an instructor for trainingId={}", post.getTrainingId());
                return;
            }
            forumEventPublisher.publishTrainingPostCreated(post, instructorId);
        } catch (Exception e) {
            log.error("Failed to notify the assigned instructor for postId={}: {}", post.getId(), e.getMessage());
        }
    }

    /**
     * Ne fait JAMAIS confiance à une imbrication déclarée par le client
     * (ex: "ce videoId appartient à ce chapterId"). Chaque id fourni est
     * validé par un appel au service propriétaire, et l'id parent réel
     * renvoyé par ce service est celui qui est stocké — jamais celui du
     * payload client.
     */
    private ResolvedRefs resolveAndValidateReferences(ForumPostCreateRequest request, String token) {
        if (request.getType() == ForumPostType.COURSE) {
            return resolveCourseRefs(request, token);
        }
        return resolveTrainingRefs(request, token);
    }

    private ResolvedRefs resolveCourseRefs(ForumPostCreateRequest request, String token) {
        if (request.getCourseId() == null || request.getCourseId().isBlank()) {
            throw new InvalidForumReferenceException("courseId is required for a COURSE-type post.");
        }
        courseServiceClient.getCourseStrict(request.getCourseId(), token);// valide l'existence, lève sinon

        String resolvedChapterId = null;
        String resolvedVideoId = null;

        if (request.getVideoId() != null) {
            CourseServiceClient.VideoRef videoRef = courseServiceClient.getVideoRef(request.getVideoId(), token);
            if (request.getChapterId() != null && !request.getChapterId().equals(videoRef.chapterId())) {
                throw new InvalidForumReferenceException("videoId does not belong to the specified chapterId.");
            }
            CourseServiceClient.ChapterRef chapterRef = courseServiceClient.getChapterRef(videoRef.chapterId(), token);
            if (!chapterRef.courseId().equals(request.getCourseId())) {
                throw new InvalidForumReferenceException("The referenced chapter/video does not belong to courseId.");
            }
            resolvedChapterId = videoRef.chapterId();
            resolvedVideoId = request.getVideoId();
        } else if (request.getChapterId() != null) {
            CourseServiceClient.ChapterRef chapterRef = courseServiceClient.getChapterRef(request.getChapterId(), token);
            if (!chapterRef.courseId().equals(request.getCourseId())) {
                throw new InvalidForumReferenceException("The referenced chapter does not belong to courseId.");
            }
            resolvedChapterId = request.getChapterId();
        }

        return new ResolvedRefs(request.getCourseId(), resolvedChapterId, resolvedVideoId, null, null, null);
    }

    private ResolvedRefs resolveTrainingRefs(ForumPostCreateRequest request, String token) {
        if (request.getTrainingId() == null || request.getTrainingId().isBlank()) {
            throw new InvalidForumReferenceException("trainingId is required for a TRAINING-type post.");
        }
        trainingServiceClient.getTrainingRef(request.getTrainingId(), token); // valide l'existence, lève sinon

        String resolvedLiveSessionId = null;
        String resolvedRecordingId = null;

        if (request.getRecordingId() != null) {
            TrainingServiceClient.RecordingRef recordingRef = trainingServiceClient.getRecordingRef(request.getRecordingId(), token);
            if (request.getLiveSessionId() != null && !request.getLiveSessionId().equals(recordingRef.sessionId())) {
                throw new InvalidForumReferenceException("recordingId does not belong to the specified liveSessionId.");
            }
            TrainingServiceClient.LiveSessionRef sessionRef = trainingServiceClient.getLiveSessionRef(recordingRef.sessionId(), token);
            if (!sessionRef.trainingId().equals(request.getTrainingId())) {
                throw new InvalidForumReferenceException("The referenced session/recording does not belong to trainingId.");
            }
            resolvedLiveSessionId = recordingRef.sessionId();
            resolvedRecordingId = request.getRecordingId();
        } else if (request.getLiveSessionId() != null) {
            TrainingServiceClient.LiveSessionRef sessionRef = trainingServiceClient.getLiveSessionRef(request.getLiveSessionId(), token);
            if (!sessionRef.trainingId().equals(request.getTrainingId())) {
                throw new InvalidForumReferenceException("The referenced live session does not belong to trainingId.");
            }
            resolvedLiveSessionId = request.getLiveSessionId();
        }

        return new ResolvedRefs(null, null, null, request.getTrainingId(), resolvedLiveSessionId, resolvedRecordingId);
    }

    /**
     * Porte-valeur purement interne (jamais sérialisé, jamais exposé) —
     * un record reste idiomatique ici, contrairement aux DTO publics de
     * l'API qui suivent la convention Lombok @Data du reste du projet.
     */
    private record ResolvedRefs(String courseId, String chapterId, String videoId,
                                String trainingId, String liveSessionId, String recordingId) {}

    // ============================== READ ==============================

    public ForumPostResponse getPost(String postId, AuthenticatedUser user) {
        ForumPost post = findOrThrow(postId);
        return buildResponse(post, user);
    }

    public PageResponse<ForumPostResponse> listPosts(ForumPostType type, String courseId, String trainingId,
                                                     String authorId, String sort, int page, int size,
                                                     AuthenticatedUser user) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();
        if (type != null) criteria.add(Criteria.where("type").is(type));
        if (courseId != null) criteria.add(Criteria.where("courseId").is(courseId));
        if (trainingId != null) criteria.add(Criteria.where("trainingId").is(trainingId));
        if (authorId != null) criteria.add(Criteria.where("authorId").is(authorId));
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, ForumPost.class);

        Sort sortSpec = "popular".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.DESC, "upvoteCount").and(Sort.by(Sort.Direction.DESC, "createdAt"))
                : Sort.by(Sort.Direction.DESC, "createdAt");

        query.with(sortSpec).with(PageRequest.of(page, size));
        List<ForumPost> posts = mongoTemplate.find(query, ForumPost.class);

        List<ForumPostResponse> content = buildResponsesBatched(posts, user);
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }

    // ============================== UPDATE ==============================

    /**
     * Update partiel — même convention que RecordingService.updateMetadata()
     * côté TRAINING-SERVICE : seuls les champs non-null du payload sont
     * appliqués. Envoyer uniquement { "body": "..." } laisse le titre
     * inchangé.
     */
    public ForumPostResponse updatePost(String postId, ForumPostUpdateRequest request, AuthenticatedUser user) {
        ForumPost post = findOrThrow(postId);
        assertOwnerOrAdmin(post.getAuthorId(), user);

        if (request.getTitle() != null) post.setTitle(request.getTitle());
        if (request.getBody() != null) post.setBody(request.getBody());
        post.setUpdatedAt(LocalDateTime.now());
        post = postRepository.save(post);

        return buildResponse(post, user);
    }

    // ============================== DELETE ==============================

    public void deletePost(String postId, AuthenticatedUser user) {
        ForumPost post = findOrThrow(postId);
        assertOwnerOrAdmin(post.getAuthorId(), user);

        commentRepository.deleteByPostId(postId);
        bookmarkRepository.deleteByPostId(postId);
        upvoteRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    // ============================== Helpers (package-visible pour ForumBookmarkService) ==============================

    ForumPost findOrThrow(String postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ForumPostNotFoundException("Forum post not found: " + postId));
    }

    void assertOwnerOrAdmin(String resourceAuthorId, AuthenticatedUser user) {
        if ("ADMIN".equals(user.role())) return;
        if (!resourceAuthorId.equals(user.userId())) {
            throw new ForumAccessDeniedException("You are not the author of this resource.");
        }
    }

    public ForumPostResponse buildResponse(ForumPost post, AuthenticatedUser user) {
        ForumAuthorSummary author = userServiceClient.getBasicInfo(post.getAuthorId(), user.token());

        ForumReferenceSummary course = null, chapter = null, video = null;
        ForumReferenceSummary training = null, liveSession = null, recording = null;

        if (post.getType() == ForumPostType.COURSE) {
            course = courseServiceClient.getCourse(post.getCourseId(), user.token());
            if (post.getChapterId() != null) chapter = courseServiceClient.getChapter(post.getChapterId(), user.token());
            if (post.getVideoId() != null) video = courseServiceClient.getVideo(post.getVideoId(), user.token());
        } else {
            training = trainingServiceClient.getTraining(post.getTrainingId(), user.token());
            if (post.getLiveSessionId() != null) liveSession = trainingServiceClient.getLiveSession(post.getLiveSessionId(), user.token());
            if (post.getRecordingId() != null) recording = trainingServiceClient.getRecording(post.getRecordingId(), user.token());
        }

        boolean bookmarked = bookmarkRepository.existsByUserIdAndPostId(user.userId(), post.getId());
        boolean upvoted = upvoteRepository.existsByUserIdAndPostId(user.userId(), post.getId());

        return forumPostMapper.toResponse(post, author, course, chapter, video, training, liveSession, recording, bookmarked, upvoted);
    }

    /**
     * Version "liste" de buildResponse : dé-duplique les appels sortants
     * (auteur / cours / training ...) par id sur la page en cours, plutôt
     * que de refaire un appel HTTP par post — utile car aucun endpoint de
     * lookup batch n'existe côté USER/COURSE/TRAINING-SERVICE aujourd'hui.
     *
     * CORRIGÉ : les caches ci-dessous utilisaient Map<String, ForumXxx> avec
     * computeIfAbsent(). Or getCourse()/getChapter()/getVideo()/getTraining()/
     * getLiveSession()/getRecording()/getBasicInfo() sont toutes des méthodes
     * TOLÉRANTES qui renvoient null en cas d'échec réseau — et computeIfAbsent()
     * ne mémorise jamais un résultat null (comportement documenté du JDK). Sous
     * une panne amont, chaque post référençant la même ressource injoignable
     * redéclenchait donc un appel HTTP au lieu d'un seul par page — l'inverse
     * de ce que ce cache est censé faire. Fix : les caches stockent désormais
     * Optional<...> (via Optional.ofNullable), donc un échec est lui aussi
     * mémorisé pour la durée de la page.
     */
    private List<ForumPostResponse> buildResponsesBatched(List<ForumPost> posts, AuthenticatedUser user) {
        if (posts.isEmpty()) return List.of();

        List<String> postIds = posts.stream().map(ForumPost::getId).toList();
        Set<String> bookmarkedIds = bookmarkRepository.findByUserIdAndPostIdIn(user.userId(), postIds)
                .stream().map(b -> b.getPostId()).collect(Collectors.toSet());
        Set<String> upvotedIds = upvoteRepository.findByUserIdAndPostIdIn(user.userId(), postIds)
                .stream().map(u -> u.getPostId()).collect(Collectors.toSet());

        Map<String, Optional<ForumAuthorSummary>> authorCache = new HashMap<>();
        Map<String, Optional<ForumReferenceSummary>> refCache = new HashMap<>();

        List<ForumPostResponse> result = new ArrayList<>(posts.size());
        for (ForumPost post : posts) {
            ForumAuthorSummary author = authorCache.computeIfAbsent(post.getAuthorId(),
                    id -> Optional.ofNullable(userServiceClient.getBasicInfo(id, user.token()))).orElse(null);

            ForumReferenceSummary course = null, chapter = null, video = null;
            ForumReferenceSummary training = null, liveSession = null, recording = null;

            if (post.getType() == ForumPostType.COURSE) {
                course = refCache.computeIfAbsent("course:" + post.getCourseId(),
                        k -> Optional.ofNullable(courseServiceClient.getCourse(post.getCourseId(), user.token()))).orElse(null);
                if (post.getChapterId() != null) {
                    chapter = refCache.computeIfAbsent("chapter:" + post.getChapterId(),
                            k -> Optional.ofNullable(courseServiceClient.getChapter(post.getChapterId(), user.token()))).orElse(null);
                }
                if (post.getVideoId() != null) {
                    video = refCache.computeIfAbsent("video:" + post.getVideoId(),
                            k -> Optional.ofNullable(courseServiceClient.getVideo(post.getVideoId(), user.token()))).orElse(null);
                }
            } else {
                training = refCache.computeIfAbsent("training:" + post.getTrainingId(),
                        k -> Optional.ofNullable(trainingServiceClient.getTraining(post.getTrainingId(), user.token()))).orElse(null);
                if (post.getLiveSessionId() != null) {
                    liveSession = refCache.computeIfAbsent("session:" + post.getLiveSessionId(),
                            k -> Optional.ofNullable(trainingServiceClient.getLiveSession(post.getLiveSessionId(), user.token()))).orElse(null);
                }
                if (post.getRecordingId() != null) {
                    recording = refCache.computeIfAbsent("recording:" + post.getRecordingId(),
                            k -> Optional.ofNullable(trainingServiceClient.getRecording(post.getRecordingId(), user.token()))).orElse(null);
                }
            }

            boolean bookmarked = bookmarkedIds.contains(post.getId());
            boolean upvoted = upvotedIds.contains(post.getId());

            result.add(forumPostMapper.toResponse(post, author, course, chapter, video, training, liveSession, recording, bookmarked, upvoted));
        }
        return result;
    }
}