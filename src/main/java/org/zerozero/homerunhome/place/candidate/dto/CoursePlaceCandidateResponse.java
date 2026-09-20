package org.zerozero.homerunhome.place.candidate.dto;

import java.util.List;
import org.zerozero.homerunhome.place.explore.dto.PlaceCoordinate;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreScope;

public record CoursePlaceCandidateResponse(
        PlaceExploreScope scope,
        PlaceCoordinate center,
        int maxDistanceMeters,
        List<CoursePlaceCandidate> places) {}
