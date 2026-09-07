package projecteLearning.service;

import org.springframework.stereotype.Service;
import projecteLearning.exception.InvalidPresetAvatarException;

import java.util.List;

@Service
public class PresetAvatarService {

    public record PresetAvatar(String id, String url) {}

    private static final List<PresetAvatar> PRESETS = List.of(
            // -- 6 avatars masculins (coiffures courtes) --
            new PresetAvatar("avatar-01", url("Milo", "top=shortFlat&mouth=smile&facialHairProbability=0")),
            new PresetAvatar("avatar-02", url("Leo", "top=shortRound&mouth=default&facialHairProbability=0")),
            new PresetAvatar("avatar-03", url("Kofi", "top=shortWaved&mouth=twinkle&facialHairProbability=0")),
            new PresetAvatar("avatar-04", url("Omar", "top=theCaesar&mouth=smile&facialHairProbability=0")),
            new PresetAvatar("avatar-05", url("Adam", "top=theCaesarAndSidePart&mouth=smile&facialHair=beardLight&facialHairProbability=100")),
            new PresetAvatar("avatar-06", url("Felix", "top=shortCurly&mouth=default&facialHairProbability=0")),

            // -- 6 avatars féminins (coiffures longues) --
            new PresetAvatar("avatar-07", url("Aneka", "top=bob&mouth=smile&facialHairProbability=0")),
            new PresetAvatar("avatar-08", url("Zoe", "top=bun&mouth=twinkle&facialHairProbability=0")),
            new PresetAvatar("avatar-09", url("Nina", "top=curly&mouth=smile&facialHairProbability=0")),
            new PresetAvatar("avatar-10", url("Ines", "top=longButNotTooLong&mouth=default&facialHairProbability=0")),
            new PresetAvatar("avatar-11", url("Salma", "top=straight01&mouth=smile&facialHairProbability=0")),
            new PresetAvatar("avatar-12", url("Yara", "top=straight02&mouth=twinkle&facialHairProbability=0"))
    );

    public List<PresetAvatar> getAll() {
        return PRESETS;
    }

    public PresetAvatar findById(String id) {
        return PRESETS.stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new InvalidPresetAvatarException("Unknown avatar id: " + id));
    }

    private static String url(String seed, String forcedParams) {
        return "https://api.dicebear.com/10.x/avataaars/svg?seed=" + seed + "&" + forcedParams;
    }
}