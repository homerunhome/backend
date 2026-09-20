package org.zerozero.homerunhome.place.recommendation.dto;

import java.util.List;
import org.zerozero.homerunhome.place.dto.MapBounds;
import org.zerozero.homerunhome.place.dto.PlaceCategory;
import org.zerozero.homerunhome.place.explore.dto.PlaceCoordinate;

public record PlaceRecommendationRequest(
        List<PlaceCategory> categories,
        String keyword,
        PlaceCoordinate referencePoint,
        Integer maxDistanceMeters,
        MapBounds courseBounds,
        Integer limit) {}
