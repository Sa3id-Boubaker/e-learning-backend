package projecteLearning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projecteLearning.dto.UserBasicInfoResponse;
import projecteLearning.exception.UserNotFoundException;
import projecteLearning.models.User;
import projecteLearning.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserLookupService {

    private final UserRepository userRepository;

    public UserBasicInfoResponse getBasicInfo(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        return UserBasicInfoResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .profileImage(user.getProfileImage())
                .role(user.getRole().name())
                .build();
    }
}