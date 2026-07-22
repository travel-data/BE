package com.nigahae.security.controller;

import com.nigahae.security.jwt.AuthenticatedUser;
import com.nigahae.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthControllerTest {

    @Test
    void returnsAuthenticatedUserClaims() {
        AuthController controller = new AuthController(mock(JwtTokenProvider.class));

        AuthController.MeResponse response = controller.me(
                new AuthenticatedUser("user@example.com", "홍길동")
        );

        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.nickname()).isEqualTo("홍길동");
    }
}
