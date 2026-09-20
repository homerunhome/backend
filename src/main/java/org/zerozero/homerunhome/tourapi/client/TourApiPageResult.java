package org.zerozero.homerunhome.tourapi.client;

import java.util.List;

public record TourApiPageResult<T>(List<T> items, int page, int size, int totalCount) {

    public TourApiPageResult {
        items = List.copyOf(items);
    }
}
