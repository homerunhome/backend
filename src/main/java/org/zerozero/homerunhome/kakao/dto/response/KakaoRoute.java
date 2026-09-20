package org.zerozero.homerunhome.kakao.dto.response;

import java.time.Duration;

public record KakaoRoute(
        TravelMode travelMode,
        int distanceMeters,
        Duration duration,
        int taxiFareWon,
        int tollFareWon) {

    public enum TravelMode {
        CAR
    }
}
