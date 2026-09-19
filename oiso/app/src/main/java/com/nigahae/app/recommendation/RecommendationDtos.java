package com.nigahae.app.recommendation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;
import java.util.List;

public final class RecommendationDtos {
    private RecommendationDtos() {}

    public record RecommendationRequest(
            Object travelTime, Object travelCompanion, Object preferredTravelTheme,
            Object transportationMode, LocalDate travelStartDate,
            List<Long> savedSpotIds, List<Long> savedNearbyPlaceIds,
            List<Long> activeFestivalSpotIds, String departureCategory,
            String departurePlaceId, Boolean withPet) {

        @JsonIgnore
        @AssertTrue(message = "출발 장소 유형과 ID는 함께 입력해야 합니다.")
        public boolean isDepartureReferenceValid() {
            return (departureCategory == null) == (departurePlaceId == null);
        }
    }

    public record RecommendationResponse(
            String concept,
            Integer totalSpotCount,
            Integer totalDurationMinute,
            List<RecommendationItem> items) {}

    public record RecommendationItem(
            Integer itemOrder, Integer dayNumber, String category, Long spotId,
            Long nearbyPlaceId, Double latitude, Double longitude,
            String transportType) {}
}
