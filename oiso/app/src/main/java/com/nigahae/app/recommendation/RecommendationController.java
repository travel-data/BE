package com.nigahae.app.recommendation;

import com.nigahae.app.recommendation.RecommendationDtos.RecommendationRequest;
import com.nigahae.app.recommendation.RecommendationDtos.RecommendationResponse;
import com.nigahae.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {
    private final RecommendationService recommendationService;

    @PostMapping("/courses")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "travelTime": "ONE_DAY",
                      "travelCompanion": "FAMILY",
                      "preferredTravelTheme": "HISTORY_CULTURE",
                      "transportationMode": "CAR",
                      "departureCategory": "PRESET",
                      "departurePlaceId": "GYEONGJU_STATION",
                      "withPet": false
                    }
                    """))
    )
    @Operation(summary = "선호정보 기반 관광코스 추천", description = "Spring이 내부 추천 서버로 요청을 중계하고 추천 결과를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천 성공"),
            @ApiResponse(responseCode = "503", description = "추천 서버 사용 불가"),
            @ApiResponse(responseCode = "504", description = "추천 서버 응답 시간 초과")
    })
    public CommonResponse<RecommendationResponse> recommend(
            @Valid @RequestBody RecommendationRequest request) {
        return CommonResponse.success(recommendationService.recommend(request));
    }
}
