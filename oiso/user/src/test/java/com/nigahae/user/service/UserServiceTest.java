package com.nigahae.user.service;

import com.nigahae.common.constant.ErrorCode;
import com.nigahae.common.exception.CustomException;
import com.nigahae.user.AuthenticatedUser;
import com.nigahae.user.controller.dto.UserPreferencesRequest;
import com.nigahae.user.controller.dto.UserPreferencesResponse;
import com.nigahae.user.domain.User;
import com.nigahae.user.domain.preference.PreferredTravelTheme;
import com.nigahae.user.domain.preference.TransportationMode;
import com.nigahae.user.domain.preference.TravelCompanion;
import com.nigahae.user.domain.preference.TravelTime;
import com.nigahae.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserServiceTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final UserService service = new UserService(repository);

    @Test
    void registersFirstSocialLogin() {
        when(repository.findByProviderAndProviderUserId("kakao", "123")).thenReturn(Optional.empty());
        when(repository.save(any(User.class))).thenReturn(User.builder().userId(7L).provider("kakao")
                .providerUserId("123").nickname("첫로그인").build());
        assertThat(service.loginOrRegister("kakao", "123", "첫로그인", null, null))
                .isEqualTo(new AuthenticatedUser(7L, "첫로그인"));
        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(repository).save(savedUser.capture());
        assertThat(savedUser.getValue().getHasTravelPlan()).isFalse();
    }

    @Test
    void updatesExistingUserWithoutDuplicateInsert() {
        User existing = User.builder().userId(7L).provider("kakao").providerUserId("123")
                .nickname("이전").build();
        when(repository.findByProviderAndProviderUserId("kakao", "123")).thenReturn(Optional.of(existing));
        AuthenticatedUser result = service.loginOrRegister("kakao", "123", "변경", "new@example.com", "image");
        assertThat(result).isEqualTo(new AuthenticatedUser(7L, "변경"));
        assertThat(existing.getEmail()).isEqualTo("new@example.com");
        verify(repository, never()).save(any());
    }

    @Test
    void updatesPreferencesUsingFixedDatabaseCodes() {
        User existing = User.builder().userId(7L).build();
        UserPreferencesRequest request = new UserPreferencesRequest(
                true,
                TravelTime.ONE_DAY,
                TravelCompanion.PARTNER,
                PreferredTravelTheme.HISTORY_CULTURE,
                TransportationMode.WALK_PUBLIC_TRANSIT
        );
        when(repository.findById(7L)).thenReturn(Optional.of(existing));

        UserPreferencesResponse response = service.updatePreferences(7L, request);

        assertThat(existing.getHasTravelPlan()).isTrue();
        assertThat(existing.getTravelTime()).isEqualTo(2L);
        assertThat(existing.getTravelCompanion()).isEqualTo(2L);
        assertThat(existing.getPreferredTravelTheme()).isEqualTo(1L);
        assertThat(existing.getTransportationMode()).isEqualTo(1L);
        assertThat(response.travelTime()).isEqualTo(TravelTime.ONE_DAY);
        assertThat(response.travelCompanion()).isEqualTo(TravelCompanion.PARTNER);
        assertThat(response.preferredTravelTheme()).isEqualTo(PreferredTravelTheme.HISTORY_CULTURE);
        assertThat(response.transportationMode()).isEqualTo(TransportationMode.WALK_PUBLIC_TRANSIT);
        verify(repository, never()).save(any());
    }

    @Test
    void clearsOptionalPreferenceValuesWhenTheyAreNull() {
        User existing = User.builder()
                .userId(7L)
                .preferredTravelTheme(3L)
                .build();
        UserPreferencesRequest request = new UserPreferencesRequest(
                false,
                TravelTime.HALF_DAY,
                TravelCompanion.ALONE,
                null,
                TransportationMode.CAR
        );
        when(repository.findById(7L)).thenReturn(Optional.of(existing));

        UserPreferencesResponse response = service.updatePreferences(7L, request);

        assertThat(existing.getPreferredTravelTheme()).isNull();
        assertThat(response.preferredTravelTheme()).isNull();
    }

    @Test
    void rejectsPreferenceUpdateForUnknownUser() {
        UserPreferencesRequest request = new UserPreferencesRequest(
                true,
                TravelTime.ONE_DAY,
                TravelCompanion.FRIENDS,
                null,
                TransportationMode.BICYCLE
        );
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePreferences(99L, request))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }
}
