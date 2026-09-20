package org.zerozero.homerunhome.place.explore.dto;

import org.zerozero.homerunhome.place.dto.MapBounds;
import org.zerozero.homerunhome.place.dto.PlaceCategory;

public record PlaceExploreRequest(
        PlaceExploreScope scope,
        PlaceCategory category,
        String keyword,
        PlaceCoordinate center,
        MapBounds bounds,
        DaejeonHotspot hotspot,
        Integer page,
        Integer size) {}
