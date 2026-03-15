package com.paulo.pricemonitor.security;

/**
 * Principal armazenado no SecurityContext após validação do JWT.
 */
public record AuthenticatedUser(Long userId, String email) {}
