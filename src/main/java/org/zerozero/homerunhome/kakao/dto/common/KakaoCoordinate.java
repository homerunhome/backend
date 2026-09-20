package org.zerozero.homerunhome.kakao.dto.common;

import java.math.BigDecimal;
import java.util.Objects;

public record KakaoCoordinate(BigDecimal longitude, BigDecimal latitude) {

    public KakaoCoordinate {
        Objects.requireNonNull(longitude, "longitude must not be null");
        Objects.requireNonNull(latitude, "latitude must not be null");
        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
    }
}
