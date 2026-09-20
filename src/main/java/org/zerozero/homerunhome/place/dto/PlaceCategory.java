package org.zerozero.homerunhome.place.dto;

import java.util.Arrays;

public enum PlaceCategory {
    MART("MT1"),
    CONVENIENCE_STORE("CS2"),
    CHILDCARE("PS3"),
    SCHOOL("SC4"),
    ACADEMY("AC5"),
    PARKING("PK6"),
    GAS_STATION("OL7"),
    SUBWAY_STATION("SW8"),
    BANK("BK9"),
    CULTURAL_FACILITY("CT1"),
    REAL_ESTATE("AG2"),
    PUBLIC_INSTITUTION("PO3"),
    TOURIST_ATTRACTION("AT4"),
    ACCOMMODATION("AD5"),
    FOOD("FD6"),
    CAFE("CE7"),
    HOSPITAL("HP8"),
    PHARMACY("PM9"),
    OTHER("");

    private final String kakaoCode;

    PlaceCategory(String kakaoCode) {
        this.kakaoCode = kakaoCode;
    }

    public String kakaoCode() {
        return kakaoCode;
    }

    public boolean searchable() {
        return this != OTHER;
    }

    public static PlaceCategory fromKakaoCode(String code) {
        return Arrays.stream(values())
                .filter(category -> category.searchable() && category.kakaoCode.equals(code))
                .findFirst()
                .orElse(OTHER);
    }
}
