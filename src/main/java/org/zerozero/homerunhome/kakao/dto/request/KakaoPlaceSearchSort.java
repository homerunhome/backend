package org.zerozero.homerunhome.kakao.dto.request;

public enum KakaoPlaceSearchSort {
    ACCURACY("accuracy"),
    DISTANCE("distance");

    private final String apiValue;

    KakaoPlaceSearchSort(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }
}
