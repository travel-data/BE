package com.nigahae.user.controller;

import com.nigahae.common.constant.ErrorCode;
import com.nigahae.common.exception.CustomException;
import com.nigahae.common.response.CommonResponse;
import com.nigahae.user.AuthenticatedUser;
import com.nigahae.user.controller.dto.UserPreferencesRequest;
import com.nigahae.user.controller.dto.UserPreferencesResponse;
import com.nigahae.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/preferences")
@Tag(name = "여행 선호정보", description = "인증 사용자의 초기 여행 선호정보 API")
public class UserPreferencesController {

    private final UserService userService;

    @PatchMapping
    @Operation(
            summary = "초기 여행 선호정보 저장",
            description = """
                    JWT 쿠키로 인증된 사용자의 선호정보 전체를 저장합니다.
                    필수 선택값은 모두 문자열 enum으로 보내야 하며, preferredTravelTheme은 null을 허용합니다.
                    출발 장소는 사용자 선호정보에 저장하지 않고 코스 추천 요청에 장소 ID로 전달합니다.

                    화면 선택값 → 요청 enum 매핑:

                    | 요청 필드 | 화면 선택값 | 전송 값 |
                    |---|---|---|
                    | travelTime | 반나절 | HALF_DAY |
                    | travelTime | 하루종일 | ONE_DAY |
                    | travelTime | 1박 2일 | ONE_NIGHT_TWO_DAYS |
                    | travelTime | 2박 3일 | TWO_NIGHTS_THREE_DAYS |
                    | travelTime | 3박 4일 | THREE_NIGHTS_FOUR_DAYS |
                    | travelCompanion | 혼자 | ALONE |
                    | travelCompanion | 연인과 함께 | PARTNER |
                    | travelCompanion | 친구와 함께 | FRIENDS |
                    | travelCompanion | 가족과 함께 | FAMILY |
                    | preferredTravelTheme | 역사 및 문화탐방 | HISTORY_CULTURE |
                    | preferredTravelTheme | 자연 및 경관 | NATURE_SCENERY |
                    | preferredTravelTheme | 먹거리 탐방 | FOOD |
                    | transportationMode | 도보 및 대중교통 | WALK_PUBLIC_TRANSIT |
                    | transportationMode | 자전거 | BICYCLE |
                    | transportationMode | 자동차 | CAR |
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "선호정보 저장 성공"),
            @ApiResponse(responseCode = "400", description = "선호정보 저장 실패: 필수값 또는 enum 검증 오류"),
            @ApiResponse(responseCode = "401", description = "선호정보 저장 실패: 인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "선호정보 저장 실패: 사용자를 찾을 수 없음")
    })
    public CommonResponse<UserPreferencesResponse> updatePreferences(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "초기 여행 선호정보 전체 입력",
                    content = @Content(
                            schema = @Schema(implementation = UserPreferencesRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "모든 항목 입력",
                                            value = """
                                                    {
                                                      "hasTravelPlan": true,
                                                      "travelTime": "ONE_DAY",
                                                      "travelCompanion": "PARTNER",
                                                      "preferredTravelTheme": "HISTORY_CULTURE",
                                                      "transportationMode": "WALK_PUBLIC_TRANSIT"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "선택 항목 없음",
                                            value = """
                                                    {
                                                      "hasTravelPlan": false,
                                                      "travelTime": "HALF_DAY",
                                                      "travelCompanion": "ALONE",
                                                      "preferredTravelTheme": null,
                                                      "transportationMode": "CAR"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @Valid @RequestBody UserPreferencesRequest request
    ) {
        if (authenticatedUser == null) {
            throw new CustomException(ErrorCode.AUTH_ERROR);
        }

        return CommonResponse.success(
                userService.updatePreferences(authenticatedUser.userId(), request)
        );
    }
}
