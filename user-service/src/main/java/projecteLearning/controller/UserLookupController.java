package projecteLearning.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projecteLearning.dto.UserBasicInfoResponse;
import projecteLearning.service.UserLookupService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserLookupController {

    private final UserLookupService userLookupService;

    @GetMapping("/{id}/basic-info")
    public ResponseEntity<UserBasicInfoResponse> getBasicInfo(@PathVariable String id) {
        return ResponseEntity.ok(userLookupService.getBasicInfo(id));
    }
}