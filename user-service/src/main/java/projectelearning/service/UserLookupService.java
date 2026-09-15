package projectelearning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projectelearning.dto.UserBasicInfoResponse;
import projectelearning.exception.UserNotFoundException;
import projectelearning.models.User;
import projectelearning.repository.UserRepository;

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