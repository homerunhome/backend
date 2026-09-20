package org.zerozero.homerunhome.tourism.dto;

import java.util.List;

public record TourismContentListResponse(
        TourismContentType contentType,
        List<TourismContentSummaryResponse> contents,
        int page,
        int size,
        int totalCount,
        boolean hasNext) {

    public TourismContentListResponse {
        contents = List.copyOf(contents);
    }
}
