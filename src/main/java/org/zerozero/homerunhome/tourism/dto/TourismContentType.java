package org.zerozero.homerunhome.tourism.dto;

import java.util.Arrays;

public enum TourismContentType {
    TOURIST_ATTRACTION("12"),
    CULTURAL_FACILITY("14"),
    FESTIVAL_EVENT("15"),
    TRAVEL_COURSE("25"),
    LEISURE_SPORTS("28"),
    UNKNOWN("");

    private final String tourApiId;

    TourismContentType(String tourApiId) {
        this.tourApiId = tourApiId;
    }

    public String tourApiId() {
        return tourApiId;
    }

    public boolean searchable() {
        return this != UNKNOWN;
    }

    public static TourismContentType fromTourApiId(String id) {
        return Arrays.stream(values())
                .filter(type -> type.searchable() && type.tourApiId.equals(id))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
