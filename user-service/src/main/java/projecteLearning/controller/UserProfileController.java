package projecteLearning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import projecteLearning.dto.*;
import projecteLearning.security.UserPrincipal;
import projecteLearning.service.UserProfileService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userProfileService.getCurrentProfile(principal));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                             @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateProfile(principal, request));
    }

    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> updateProfileImage(@AuthenticationPrincipal UserPrincipal principal,
                                                                  @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userProfileService.updateProfileImage(principal, file));
    }

    @PutMapping("/me/password")
    public ResponseEntity<MessageResponse> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                          @Valid @RequestBody ChangePasswordRequest request) {
        userProfileService.changePassword(principal, request);
        return ResponseEntity.ok(new MessageResponse("Password changed successfully."));
    }

    @PutMapping("/me/profile-image/preset")
    public ResponseEntity<UserProfileResponse> setPresetAvatar(@AuthenticationPrincipal UserPrincipal principal,
                                                               @Valid @RequestBody SetPresetAvatarRequest request) {
        return ResponseEntity.ok(userProfileService.setPresetAvatar(principal, request));
    }

    @PutMapping("/me/force-change-password")
    public ResponseEntity<MessageResponse> forceChangePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                               @Valid @RequestBody ForceChangePasswordRequest request) {
        return ResponseEntity.ok(userProfileService.forceChangePassword(principal, request));
    }
}