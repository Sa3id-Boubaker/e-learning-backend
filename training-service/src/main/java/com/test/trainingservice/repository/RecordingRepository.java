package com.test.trainingservice.repository;

import com.test.trainingservice.entity.Recording;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

import java.util.Collection;

public interface RecordingRepository extends MongoRepository<Recording, String> {

    Optional<Recording> findBySessionId(String sessionId);

    boolean existsBySessionId(String sessionId);

    List<SessionIdOnly> findBySessionIdIn(Collection<String> sessionIds);

    /** Used by TrainingService.getDeletionImpact() to report the recording count before a cascade delete. */
    long countBySessionIdIn(Collection<String> sessionIds);

    /**
     * Full entities (unlike the SessionIdOnly projection above) — TrainingService.delete() needs
     * videoPublicId on each one to clean up the underlying Cloudinary video assets before the
     * documents themselves are deleted.
     */
    List<Recording> findAllBySessionIdIn(Collection<String> sessionIds);

    /** Used by TrainingService.delete() to cascade-delete every recording under a training. */
    void deleteBySessionIdIn(Collection<String> sessionIds);

    interface SessionIdOnly {
        String getSessionId();
    }
}