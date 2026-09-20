package org.zerozero.homerunhome.kakao.dto.request;

import java.util.Objects;
import org.zerozero.homerunhome.kakao.dto.common.KakaoCoordinate;

public record KakaoRouteRequest(KakaoCoordinate origin, KakaoCoordinate destination) {

    public KakaoRouteRequest {
        Objects.requireNonNull(origin, "origin must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
    }
}
