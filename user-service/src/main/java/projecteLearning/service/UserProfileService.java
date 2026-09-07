package projecteLearning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import projecteLearning.dto.*;
import projecteLearning.exception.InvalidCurrentPasswordException;
import projecteLearning.exception.PasswordMismatchException;
import projecteLearning.exception.UserNotFoundException;
import projecteLearning.models.User;
import projecteLearning.repository.UserRepository;
import projecteLearning.security.UserPrincipal;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;
    private final PresetAvatarService presetAvatarService;

    public UserProfileResponse getCurrentProfile(UserPrincipal principal) {
        User user = resolveCurrentUser(principal);
        return toProfileResponse(user);
    }

    public UserProfileResponse updateProfile(UserPrincipal principal, UpdateProfileRequest request) {
        User user = resolveCurrentUser(principal);

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getBio() != null) user.setBio(request.getBio());

        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        return toProfileResponse(saved);
    }

    public UserProfileResponse updateProfileImage(UserPrincipal principal, MultipartFile file) {
        User user = resolveCurrentUser(principal);

        String previousPublicId = user.getProfileImagePublicId();

        CloudinaryService.UploadResult uploadResult = cloudinaryService.uploadProfileImage(file, user.getId());

        user.setProfileImage(uploadResult.url());
        user.setProfileImagePublicId(uploadResult.publicId());
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        cloudinaryService.deleteIfExists(previousPublicId); // nettoyage de l'ancienne image, best-effort

        return toProfileResponse(saved);
    }

    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        User user = resolveCurrentUser(principal);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCurrentPasswordException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new PasswordMismatchException("New password and confirmation do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private User resolveCurrentUser(UserPrincipal principal) {
        return userRepository.findById(principal.getUser().getId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .bio(user.getBio())
                .profileImage(user.getProfileImage())
                .role(user.getRole())
                .build();
    }

    public UserProfileResponse setPresetAvatar(UserPrincipal principal, SetPresetAvatarRequest request) {
        User user = resolveCurrentUser(principal);

        PresetAvatarService.PresetAvatar preset = presetAvatarService.findById(request.getAvatarId());

        String previousPublicId = user.getProfileImagePublicId();

        user.setProfileImage(preset.url());
        user.setProfileImagePublicId(null); // pas un upload Cloudinary : rien à suivre pour une suppression future
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        cloudinaryService.deleteIfExists(previousPublicId); // nettoie l'ancienne photo Cloudinary si l'utilisateur en avait uploadé une

        return toProfileResponse(saved);
    }

    public MessageResponse forceChangePassword(UserPrincipal principal, ForceChangePasswordRequest request) {
        User user = resolveCurrentUser(principal);

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new PasswordMismatchException("New password and confirmation do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return new MessageResponse("Password changed successfully. You can now use your account.");
    }
}