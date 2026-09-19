package com.nigahae.app.location;

import com.nigahae.app.location.PlaceRouteDtos.PlaceRouteRequest;
import com.nigahae.common.response.CommonResponse;
import com.nigahae.location.RouteCalculator.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/routes/places")
public class PlaceRouteController {
    private final PlaceRouteService placeRouteService;

    @PostMapping("/calculate")
    public CommonResponse<Result> calculate(
            @Valid @RequestBody PlaceRouteRequest request
    ) {
        return CommonResponse.success(placeRouteService.calculate(request));
    }
}
