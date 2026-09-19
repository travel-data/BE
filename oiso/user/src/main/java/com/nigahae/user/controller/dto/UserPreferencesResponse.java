package com.nigahae.user.controller.dto;

import com.nigahae.user.domain.User;
import com.nigahae.user.domain.preference.PreferredTravelTheme;
import com.nigahae.user.domain.preference.TransportationMode;
import com.nigahae.user.domain.preference.TravelCompanion;
import com.nigahae.user.domain.preference.TravelTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserPreferencesResponse", description = "저장된 사용자 여행 선호정보")
public record UserPreferencesResponse(
        @Schema(description = "여행 계획 보유 여부", example = "true")
        Boolean hasTravelPlan,

        @Schema(description = "여행 일정", example = "ONE_DAY")
        TravelTime travelTime,

        @Schema(description = "동행 유형", example = "PARTNER")
        TravelCompanion travelCompanion,

        @Schema(description = "선호 여행 테마", example = "HISTORY_CULTURE", nullable = true)
        PreferredTravelTheme preferredTravelTheme,

        @Schema(description = "이동 수단", example = "WALK_PUBLIC_TRANSIT")
        TransportationMode transportationMode
) {
    public static UserPreferencesResponse from(User user) {
        return new UserPreferencesResponse(
                user.getHasTravelPlan(),
                TravelTime.fromCode(user.getTravelTime()),
                TravelCompanion.fromCode(user.getTravelCompanion()),
                PreferredTravelTheme.fromCode(user.getPreferredTravelTheme()),
                TransportationMode.fromCode(user.getTransportationMode())
        );
    }
}
