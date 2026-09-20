package org.zerozero.homerunhome.place.candidate.dto;

import java.time.LocalDate;
import java.util.List;
import org.zerozero.homerunhome.place.explore.dto.DaejeonHotspot;
import org.zerozero.homerunhome.place.explore.dto.PlaceCoordinate;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreScope;

public record CoursePlaceCandidateRequest(
        PlaceExploreScope scope,
        List<CoursePlaceCategory> categories,
        PlaceCoordinate center,
        DaejeonHotspot hotspot,
        String keyword,
        Integer maxDistanceMeters,
        LocalDate eventDate,
        Integer limit) {}
