package com.nigahae.security.jwt;

/**
 * Access token에서 복원한 현재 로그인 사용자 정보.
 */
public record AuthenticatedUser(String email, String nickname) {
}
