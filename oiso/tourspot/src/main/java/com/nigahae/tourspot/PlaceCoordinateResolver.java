package com.nigahae.tourspot;

import com.nigahae.common.constant.ErrorCode;
import com.nigahae.common.exception.CustomException;
import com.nigahae.tourspot.domain.TourSpot;
import com.nigahae.tourspot.dto.NearbyPlaceRow;
import com.nigahae.tourspot.repository.TourSpotQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceCoordinateResolver {
    private final TourSpotQueryRepository tourSpotQueryRepository;

    public PlaceCoordinate resolve(String category, Long spotId, Long nearbyPlaceId) {
        if ("TOUR_SPOT".equals(category)) {
            TourSpot place = tourSpotQueryRepository.findById(spotId)
                    .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
            return new PlaceCoordinate(place.getLocalizedName(), place.getMapX(), place.getMapY());
        }

        NearbyPlaceRow place = tourSpotQueryRepository.findNearbyPlace(nearbyPlaceId, null)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        return new PlaceCoordinate(place.getName(), place.getMapX(), place.getMapY());
    }

    public record PlaceCoordinate(String name, Double longitude, Double latitude) {}
}
