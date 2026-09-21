package com.nigahae.location.controller;

import com.nigahae.common.response.CommonResponse;
import com.nigahae.location.dto.RouteCalculationRequest;
import com.nigahae.location.dto.RouteCalculationResponse;
import com.nigahae.location.service.LocationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/internal/routes")
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/calculate")
    public CommonResponse<RouteCalculationResponse> calculateRoute(@RequestBody RouteCalculationRequest request) {
        return new CommonResponse<>(
                CommonResponse.SUCCESS_CODE,
                "장소 간 이동 정보를 계산했습니다.",
                locationService.calculateRoute(request));
    }
}
