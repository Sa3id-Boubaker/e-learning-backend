package com.test.trainingservice.repository;

import com.test.trainingservice.entity.Training;
import com.test.trainingservice.entity.TrainingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface TrainingRepository extends MongoRepository<Training, String> {

    List<Training> findByInstructorId(String instructorId);

    List<Training> findByStatus(TrainingStatus status);

    /**
     * "Published or beyond" — used everywhere ETUDIANT visibility is decided, since a training
     * that has progressed to IN_PROGRESS/COMPLETED must stay visible, not just literally
     * PUBLISHED. Only DRAFT and CANCELLED are excluded.
     */
    List<Training> findByStatusNotIn(Collection<TrainingStatus> statuses);
}