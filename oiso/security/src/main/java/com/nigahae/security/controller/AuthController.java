package com.nigahae.security.controller;

import com.nigahae.common.constant.ErrorCode;
import com.nigahae.common.exception.CustomException;
import com.nigahae.security.jwt.AuthenticatedUser;
import com.nigahae.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.nigahae.common.constant.CommonConstant.*;
import static com.nigahae.security.util.CookieUtil.addCookie;
import static com.nigahae.security.util.CookieUtil.getCookie;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    public record MeResponse(String email, String nickname) {
    }

    @Value("${JWT_ACCESS_TOKEN_VALIDITY_SEC}")
    private int jwtAccessTokenValiditySec;

    @Value("${JWT_COOKIE_DELETE_VALIDITY_SEC}")
    private int jwtCookieDeleteValiditySec;

    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getCookie(request, REFRESH_TOKEN)
                .map(Cookie::getValue)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_REFRESH_TOKEN));

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String email = jwtTokenProvider.getEmail(refreshToken);
        String nickname = jwtTokenProvider.getNickname(refreshToken);

        String newAccessToken = jwtTokenProvider.createAccessToken(email, nickname);

        addCookie(response, ACCESS_TOKEN, newAccessToken, jwtAccessTokenValiditySec, true);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return new MeResponse(authenticatedUser.email(), authenticatedUser.nickname());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        addCookie(response, ACCESS_TOKEN, EMPTY_VALUE, jwtCookieDeleteValiditySec, true);
        addCookie(response, REFRESH_TOKEN, EMPTY_VALUE, jwtCookieDeleteValiditySec, true);

        return ResponseEntity.ok().body(SUCCESS_LOGOUT_MESSAGE);
    }
}
