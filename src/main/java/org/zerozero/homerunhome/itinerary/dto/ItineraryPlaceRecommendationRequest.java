package org.zerozero.homerunhome.itinerary.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCategory;

public record ItineraryPlaceRecommendationRequest(
        @NotEmpty List<@NotNull CoursePlaceCategory> categories,
        @Min(1) @Max(20_000) Integer maxDistanceMeters,
        @Min(1) @Max(50) Integer limit) {}
