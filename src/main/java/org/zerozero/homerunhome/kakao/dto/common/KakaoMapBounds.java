package org.zerozero.homerunhome.kakao.dto.common;

import java.util.Objects;

public record KakaoMapBounds(KakaoCoordinate southWest, KakaoCoordinate northEast) {

    public KakaoMapBounds {
        Objects.requireNonNull(southWest, "southWest must not be null");
        Objects.requireNonNull(northEast, "northEast must not be null");
        if (southWest.longitude().compareTo(northEast.longitude()) >= 0
                || southWest.latitude().compareTo(northEast.latitude()) >= 0) {
            throw new IllegalArgumentException("southWest must be located below and left of northEast");
        }
    }

    public String apiValue() {
        return southWest.longitude().toPlainString()
                + ","
                + southWest.latitude().toPlainString()
                + ","
                + northEast.longitude().toPlainString()
                + ","
                + northEast.latitude().toPlainString();
    }
}
