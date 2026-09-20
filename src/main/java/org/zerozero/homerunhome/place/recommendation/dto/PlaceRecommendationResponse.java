package org.zerozero.homerunhome.place.recommendation.dto;

import java.util.List;
import org.zerozero.homerunhome.place.dto.PlaceCategory;
import org.zerozero.homerunhome.place.dto.PlaceResponse;
import org.zerozero.homerunhome.place.explore.dto.PlaceCoordinate;

public record PlaceRecommendationResponse(
        List<PlaceCategory> categories,
        String keyword,
        PlaceCoordinate referencePoint,
        int maxDistanceMeters,
        List<PlaceResponse> places) {

    public PlaceRecommendationResponse {
        categories = List.copyOf(categories);
        places = List.copyOf(places);
    }
}
