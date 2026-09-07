package com.test.forumservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update partiel — même convention que RecordingUpdateRequest côté
 * TRAINING-SERVICE : aucun champ requis, seuls ceux fournis (non-null)
 * sont appliqués. Envoyer { "body": "..." } seul modifie uniquement le
 * corps, sans toucher au titre.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostUpdateRequest {

    private String title;
    private String body;
}