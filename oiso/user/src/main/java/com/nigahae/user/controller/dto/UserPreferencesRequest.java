package com.nigahae.user.controller.dto;

import com.nigahae.user.domain.preference.PreferredTravelTheme;
import com.nigahae.user.domain.preference.TransportationMode;
import com.nigahae.user.domain.preference.TravelCompanion;
import com.nigahae.user.domain.preference.TravelTime;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(
        name = "UserPreferencesRequest",
        description = "초기 여행 선호정보. 출발 장소는 코스별 장소 ID로 별도 전달합니다."
)
public record UserPreferencesRequest(
        @Schema(
                description = """
                        화면 선택값에 대응하는 boolean:
                        - 계획이 있어요 → true
                        - 계획이 없어요 → false
                        """,
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "여행 계획 여부는 필수입니다.")
        Boolean hasTravelPlan,

        @Schema(
                description = """
                        화면의 여행 일정 선택값에 대응하는 enum 문자열:
                        - 반나절 → HALF_DAY
                        - 하루종일 → ONE_DAY
                        - 1박 2일 → ONE_NIGHT_TWO_DAYS
                        - 2박 3일 → TWO_NIGHTS_THREE_DAYS
                        - 3박 4일 → THREE_NIGHTS_FOUR_DAYS
                        """,
                example = "ONE_DAY",
                allowableValues = {
                        "HALF_DAY",
                        "ONE_DAY",
                        "ONE_NIGHT_TWO_DAYS",
                        "TWO_NIGHTS_THREE_DAYS",
                        "THREE_NIGHTS_FOUR_DAYS"
                },
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "여행 일정은 필수입니다.")
        TravelTime travelTime,

        @Schema(
                description = """
                        화면의 동행 유형 선택값에 대응하는 enum 문자열:
                        - 혼자 → ALONE
                        - 연인과 함께 → PARTNER
                        - 친구와 함께 → FRIENDS
                        - 가족과 함께 → FAMILY
                        """,
                example = "PARTNER",
                allowableValues = {"ALONE", "PARTNER", "FRIENDS", "FAMILY"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "동행 유형은 필수입니다.")
        TravelCompanion travelCompanion,

        @Schema(
                description = """
                        화면의 선호 테마 선택값에 대응하는 enum 문자열:
                        - 역사 및 문화탐방 → HISTORY_CULTURE
                        - 자연 및 경관 → NATURE_SCENERY
                        - 먹거리 탐방 → FOOD
                        - 선택하지 않음 → null
                        """,
                example = "HISTORY_CULTURE",
                allowableValues = {"HISTORY_CULTURE", "NATURE_SCENERY", "FOOD"},
                nullable = true
        )
        PreferredTravelTheme preferredTravelTheme,

        @Schema(
                description = """
                        화면의 이동 계획 선택값에 대응하는 enum 문자열:
                        - 도보 및 대중교통 → WALK_PUBLIC_TRANSIT
                        - 자전거 → BICYCLE
                        - 자동차 → CAR
                        """,
                example = "WALK_PUBLIC_TRANSIT",
                allowableValues = {"WALK_PUBLIC_TRANSIT", "BICYCLE", "CAR"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "이동 수단은 필수입니다.")
        TransportationMode transportationMode
) {}
