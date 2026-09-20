package org.zerozero.homerunhome.place.candidate.dto;

import java.util.List;
import org.zerozero.homerunhome.place.explore.dto.PlaceCoordinate;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreScope;

public record CoursePlaceCandidateResponse(
        PlaceExploreScope scope,
        PlaceCoordinate center,
        List<PlaceCoordinate> searchCenters,
        int maxDistanceMeters,
        List<CoursePlaceCandidate> places) {

    public CoursePlaceCandidateResponse {
        searchCenters = List.copyOf(searchCenters);
        places = List.copyOf(places);
    }

    public CoursePlaceCandidateResponse(
            PlaceExploreScope scope,
            PlaceCoordinate center,
            int maxDistanceMeters,
            List<CoursePlaceCandidate> places) {
        this(
                scope,
                center,
                center == null ? List.of() : List.of(center),
                maxDistanceMeters,
                places);
    }
}
