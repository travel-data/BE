package com.nigahae.security.oauth;

import com.nigahae.security.jwt.JwtTokenProvider;
import com.nigahae.security.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

import static com.nigahae.common.constant.CommonConstant.*;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Value("${JWT_ACCESS_TOKEN_VALIDITY_SEC}")
    private int accessTokenValiditySec;

    @Value("${JWT_REFRESH_TOKEN_VALIDITY_SEC}")
    private int refreshTokenValiditySec;

    private final JwtTokenProvider jwtTokenProvider;

    private final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;

        String provider = authToken.getAuthorizedClientRegistrationId(); // google 또는 kakao
        String email = EMPTY_VALUE;
        String nickname = EMPTY_VALUE;
        String profileImageUrl = EMPTY_VALUE;

        if (KAKAO.equals(provider)) {
            Map<String, Object> kakaoAccount = oAuth2User.getAttribute(KAKAO_ACCOUNT);
            Map<String, Object> properties = oAuth2User.getAttribute(PROPERTIES);

            email = (String) kakaoAccount.get(EMAIL);
            nickname = (String) properties.get(NICKNAME);

            // 💡 카카오 프로필 이미지 추출 (properties 구조 내부 혹은 kakao_account.profile 내부에 존재)
            if (properties != null && properties.containsKey(PROFILE_IMAGE)) {
                profileImageUrl = (String) properties.get(PROFILE_IMAGE);
            } else if (kakaoAccount != null && kakaoAccount.containsKey(PROFILE)) {
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get(PROFILE);
                profileImageUrl = (String) profile.get(PROFILE_IMAGE_URL);
            }

        } else if (GOOGLE.equals(provider)) {
            email = oAuth2User.getAttribute(EMAIL);
            nickname = oAuth2User.getAttribute(NAME);

            // 💡 구글 프로필 이미지 추출 (구글은 'picture'라는 키값으로 제공합니다)
            profileImageUrl = oAuth2User.getAttribute(PICTURE);
        }

        log.info("{} {} {} {}", provider, email, nickname, profileImageUrl);

        // 회원가입 처리
        // userModuleService.loginOrRegister(email, nickname, profileImageUrl);

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(email, nickname);
        String refreshToken = jwtTokenProvider.createRefreshToken(email, nickname);

        // 💡 CookieUtil을 사용한 초간단 쿠키 생성
        CookieUtil.addCookie(response, ACCESS_TOKEN, accessToken, accessTokenValiditySec, true);
        CookieUtil.addCookie(response, REFRESH_TOKEN, refreshToken, refreshTokenValiditySec, true);

        response.sendRedirect(redirectUri);
    }
}
