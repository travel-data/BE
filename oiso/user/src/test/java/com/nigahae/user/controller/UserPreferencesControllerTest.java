package com.nigahae.user.controller;

import com.nigahae.common.constant.ErrorCode;
import com.nigahae.common.exception.CustomException;
import com.nigahae.common.exception.GlobalExceptionHandler;
import com.nigahae.user.AuthenticatedUser;
import com.nigahae.user.controller.dto.UserPreferencesRequest;
import com.nigahae.user.controller.dto.UserPreferencesResponse;
import com.nigahae.user.domain.preference.PreferredTravelTheme;
import com.nigahae.user.domain.preference.TransportationMode;
import com.nigahae.user.domain.preference.TravelCompanion;
import com.nigahae.user.domain.preference.TravelTime;
import com.nigahae.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserPreferencesControllerTest {

    private final UserService userService = mock(UserService.class);
    private final UserPreferencesController controller = new UserPreferencesController(userService);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authenticatedUserResolver(new AuthenticatedUser(42L, "사용자")))
                .build();
    }

    @Test
    void updatesPreferencesForAuthenticatedUser() throws Exception {
        UserPreferencesRequest request = validRequest();
        UserPreferencesResponse response = new UserPreferencesResponse(
                true,
                TravelTime.ONE_DAY,
                TravelCompanion.PARTNER,
                PreferredTravelTheme.HISTORY_CULTURE,
                TransportationMode.WALK_PUBLIC_TRANSIT
        );
        when(userService.updatePreferences(42L, request)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/users/me/preferences")
                        .contentType("application/json")
                        .content("""
                                {
                                  "hasTravelPlan": true,
                                  "travelTime": "ONE_DAY",
                                  "travelCompanion": "PARTNER",
                                  "preferredTravelTheme": "HISTORY_CULTURE",
                                  "transportationMode": "WALK_PUBLIC_TRANSIT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.travelTime").value("ONE_DAY"))
                .andExpect(jsonPath("$.data.travelCompanion").value("PARTNER"))
                .andExpect(jsonPath("$.data.preferredTravelTheme").value("HISTORY_CULTURE"))
                .andExpect(jsonPath("$.data.transportationMode").value("WALK_PUBLIC_TRANSIT"));

        verify(userService).updatePreferences(42L, request);
    }

    @Test
    void rejectsInvalidEnumAsValidationError() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/preferences")
                        .contentType("application/json")
                        .content("""
                                {
                                  "hasTravelPlan": true,
                                  "travelTime": "UNKNOWN",
                                  "travelCompanion": "PARTNER",
                                  "transportationMode": "CAR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.data").doesNotExist());

        verifyNoInteractions(userService);
    }

    @Test
    void rejectsMissingAuthenticationPrincipal() {
        assertThatThrownBy(() -> controller.updatePreferences(null, validRequest()))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_ERROR));
        verifyNoInteractions(userService);
    }

    private UserPreferencesRequest validRequest() {
        return new UserPreferencesRequest(
                true,
                TravelTime.ONE_DAY,
                TravelCompanion.PARTNER,
                PreferredTravelTheme.HISTORY_CULTURE,
                TransportationMode.WALK_PUBLIC_TRANSIT
        );
    }

    private HandlerMethodArgumentResolver authenticatedUserResolver(AuthenticatedUser user) {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().equals(AuthenticatedUser.class);
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    org.springframework.web.bind.support.WebDataBinderFactory binderFactory
            ) {
                return user;
            }
        };
    }
}
