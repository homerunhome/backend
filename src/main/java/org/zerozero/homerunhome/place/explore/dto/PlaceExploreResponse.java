package org.zerozero.homerunhome.place.explore.dto;

import java.util.List;
import org.zerozero.homerunhome.place.dto.PlaceResponse;

public record PlaceExploreResponse(
        PlaceExploreScope scope,
        DaejeonHotspot hotspot,
        List<PlaceResponse> places,
        int page,
        int size,
        int totalCount,
        int pageableCount,
        boolean hasNext) {

    public PlaceExploreResponse {
        places = List.copyOf(places);
    }
}
