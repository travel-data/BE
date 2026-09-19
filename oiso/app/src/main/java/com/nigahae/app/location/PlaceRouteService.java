package com.nigahae.app.location;

import com.nigahae.app.location.PlaceRouteDtos.PlaceReference;
import com.nigahae.app.location.PlaceRouteDtos.PlaceRouteRequest;
import com.nigahae.location.RouteCalculator;
import com.nigahae.location.RouteCalculator.Point;
import com.nigahae.location.RouteCalculator.Result;
import com.nigahae.tourspot.PlaceCoordinateResolver;
import com.nigahae.tourspot.PlaceCoordinateResolver.PlaceCoordinate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceRouteService {
    private final PlaceCoordinateResolver placeCoordinateResolver;
    private final RouteCalculator routeCalculator;

    public Result calculate(PlaceRouteRequest request) {
        return routeCalculator.calculate(
                resolve(request.origin()),
                resolve(request.destination()),
                request.transportType()
        );
    }

    private Point resolve(PlaceReference reference) {
        PlaceCoordinate place = placeCoordinateResolver.resolve(
                reference.category(), reference.spotId(), reference.nearbyPlaceId());
        return new Point(place.name(), place.longitude(), place.latitude());
    }
}
