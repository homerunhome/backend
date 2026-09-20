package org.zerozero.homerunhome.place.dto;

import java.util.List;

public record PlaceSearchResponse(
        List<PlaceResponse> places,
        int page,
        int size,
        int totalCount,
        int pageableCount,
        boolean hasNext) {

    public PlaceSearchResponse {
        places = List.copyOf(places);
    }
}
