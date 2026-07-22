package com.nigahae.security.util;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Optional;


/**
 * 쿠키 생성 관련 util 클래스
 * common에 하지 않은 이유는 다른 모듈에서는 쿠키와 관련된 설정을 사용할 필요가 없음
 */
public class CookieUtil {

    /**
     * 쿠키 생성 및 Response 추가
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge, boolean isHttpOnly) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .secure(true)
                .httpOnly(isHttpOnly)
                .sameSite("None")
                .maxAge(Duration.ofSeconds(maxAge))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 이름으로 쿠키 조회
     */
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }
}
