package projecteLearning.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import projecteLearning.models.Role;
import projecteLearning.models.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    long countByRole(Role role);

    @Query("{ 'role': ?0, '$or': [ " +
            "{ 'firstName': { $regex: ?1, $options: 'i' } }, " +
            "{ 'lastName': { $regex: ?1, $options: 'i' } }, " +
            "{ 'email': { $regex: ?1, $options: 'i' } } " +
            "] }")
    Page<User> searchByRole(Role role, String search, Pageable pageable);
}