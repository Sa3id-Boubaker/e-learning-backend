package com.test.forumservice.mapper;

import com.test.forumservice.dto.ForumReferenceSummary;

/**
 * Porte-valeur interne pour ForumPostMapper.toResponse(...) — regroupe les 6
 * ForumReferenceSummary (course/chapter/video pour un post COURSE, ou
 * training/liveSession/recording pour un post TRAINING) afin de ramener la
 * signature de toResponse(...) de 10 à 5 paramètres (règle Sonar java:S107,
 * max 7 paramètres). Jamais sérialisé, jamais exposé côté API — un record
 * reste idiomatique ici, sur le même principe que ForumPostService.ResolvedRefs.
 */
public record ForumPostReferences(ForumReferenceSummary course, ForumReferenceSummary chapter, ForumReferenceSummary video,
                                  ForumReferenceSummary training, ForumReferenceSummary liveSession, ForumReferenceSummary recording) {}