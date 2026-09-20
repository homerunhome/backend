package org.zerozero.homerunhome.place.candidate.dto;

import org.zerozero.homerunhome.tourism.dto.TourismContentType;

public enum CoursePlaceCategory {
    FOOD("FD6", null, null),
    CAFE("CE7", null, null),
    CONVENIENCE_STORE("CS2", null, null),
    ACTIVITY(null, "놀거리", null),
    TOURIST_ATTRACTION("AT4", null, TourismContentType.TOURIST_ATTRACTION),
    CULTURAL_FACILITY("CT1", null, TourismContentType.CULTURAL_FACILITY),
    FESTIVAL_EVENT(null, null, TourismContentType.FESTIVAL_EVENT);

    private final String kakaoCode;
    private final String defaultKeyword;
    private final TourismContentType tourismContentType;

    CoursePlaceCategory(
            String kakaoCode, String defaultKeyword, TourismContentType tourismContentType) {
        this.kakaoCode = kakaoCode;
        this.defaultKeyword = defaultKeyword;
        this.tourismContentType = tourismContentType;
    }

    public String kakaoCode() {
        return kakaoCode;
    }

    public String defaultKeyword() {
        return defaultKeyword;
    }

    public TourismContentType tourismContentType() {
        return tourismContentType;
    }

    public boolean usesKakao() {
        return kakaoCode != null || defaultKeyword != null;
    }

    public boolean usesNearbyTourApi() {
        return tourismContentType != null && this != FESTIVAL_EVENT;
    }
}
