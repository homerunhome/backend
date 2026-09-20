package org.zerozero.homerunhome.tourapi.client;

import java.util.List;
import java.util.Map;

public record TourApiPage(List<Map<String, Object>> items, int page, int size, int totalCount) {

    public TourApiPage {
        items = List.copyOf(items);
    }
}
