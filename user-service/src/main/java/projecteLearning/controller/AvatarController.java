package projecteLearning.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projecteLearning.dto.PresetAvatarResponse;
import projecteLearning.service.PresetAvatarService;

import java.util.List;

@RestController
@RequestMapping("/api/avatars")
@RequiredArgsConstructor
public class AvatarController {

    private final PresetAvatarService presetAvatarService;

    @GetMapping("/presets")
    public ResponseEntity<List<PresetAvatarResponse>> getPresets() {
        List<PresetAvatarResponse> response = presetAvatarService.getAll().stream()
                .map(p -> new PresetAvatarResponse(p.id(), p.url()))
                .toList();
        return ResponseEntity.ok(response);
    }
}