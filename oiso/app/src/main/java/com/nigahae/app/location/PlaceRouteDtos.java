package com.nigahae.app.location;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public final class PlaceRouteDtos {
    private PlaceRouteDtos() {}

    public record PlaceReference(
            @NotNull String category,
            Long spotId,
            Long nearbyPlaceId
    ) {
        @JsonIgnore
        @AssertTrue(message = "장소 유형에 맞는 ID를 하나만 입력해야 합니다.")
        public boolean isPlaceIdValid() {
            return "TOUR_SPOT".equals(category)
                    ? spotId != null && nearbyPlaceId == null
                    : ("RESTAURANT".equals(category) || "ACCOMMODATION".equals(category))
                    && spotId == null && nearbyPlaceId != null;
        }
    }

    public record PlaceRouteRequest(
            @NotNull @Valid PlaceReference origin,
            @NotNull @Valid PlaceReference destination,
            @NotNull String transportType
    ) {}
}
