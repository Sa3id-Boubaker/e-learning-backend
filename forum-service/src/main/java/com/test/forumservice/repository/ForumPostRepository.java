package com.test.forumservice.repository;

import com.test.forumservice.entity.ForumPost;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ForumPostRepository extends MongoRepository<ForumPost, String> {

    // GET /api/forum/posts combine jusqu'à 4 filtres optionnels indépendants (type, courseId,
    // trainingId, authorId) plus un choix de tri (recent/popular). Plutôt qu'une explosion
    // combinatoire de méthodes dérivées, ForumPostService construit cette requête dynamique
    // directement avec MongoTemplate/Criteria — même approche que
    // TrainingEnrollmentService.adminList() dans TRAINING-SERVICE.
    //
    // findAllById(...) (hérité de MongoRepository) est utilisé tel quel par
    // ForumPostService.listBookmarked() pour résoudre les lignes ForumBookmark en posts complets.
}