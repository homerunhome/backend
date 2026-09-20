package org.zerozero.homerunhome.tourism.dto;

import java.time.LocalDate;
import java.util.List;

public record TourismEventListResponse(
        LocalDate startDate,
        LocalDate endDate,
        List<TourismEventResponse> events,
        int page,
        int size,
        int totalCount,
        boolean hasNext) {

    public TourismEventListResponse {
        events = List.copyOf(events);
    }
}
