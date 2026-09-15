package com.test.forumservice.dto;

import com.test.forumservice.entity.ForumPostType;

/**
 * Porte-valeur interne pour ForumPostService.listPosts(...) — regroupe les 5
 * critères de filtrage/tri (type/courseId/trainingId/authorId/sort) afin de
 * ramener la signature de listPosts(...) de 8 à 4 paramètres (règle Sonar
 * java:S107, max 7 paramètres). Construit côté ForumPostController à partir
 * des @RequestParam individuels — jamais désérialisé directement depuis une
 * requête HTTP.
 */
public record ForumPostListFilter(ForumPostType type, String courseId, String trainingId, String authorId, String sort) {}