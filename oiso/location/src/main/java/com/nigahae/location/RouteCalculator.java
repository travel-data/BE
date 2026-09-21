package com.nigahae.location;

import com.nigahae.location.domain.RouteTransportType;
import com.nigahae.location.dto.CoordinateRequest;
import com.nigahae.location.dto.RouteCalculationRequest;
import com.nigahae.location.dto.RouteCalculationResponse;
import com.nigahae.location.service.LocationService;
import org.springframework.stereotype.Service;

@Service
public class RouteCalculator {
    private final LocationService locationService;

    public RouteCalculator(LocationService locationService) {
        this.locationService = locationService;
    }

    public Result calculate(Point origin, Point destination, String transportType) {
        RouteCalculationResponse response = locationService.calculateRoute(
                new RouteCalculationRequest(
                        new CoordinateRequest(origin.name(), origin.longitude(), origin.latitude()),
                        new CoordinateRequest(destination.name(), destination.longitude(), destination.latitude()),
                        RouteTransportType.valueOf(transportType)
                )
        );
        return new Result(
                origin,
                destination,
                response.transportType().name(),
                response.distanceMeters(),
                response.durationSeconds(),
                response.distanceText(),
                response.durationText(),
                response.navigationUrl()
        );
    }

    public record Point(String name, Double longitude, Double latitude) {}

    public record Result(
            Point origin,
            Point destination,
            String transportType,
            int distanceMeters,
            int durationSeconds,
            String distanceText,
            String durationText,
            String navigationUrl
    ) {}
}
