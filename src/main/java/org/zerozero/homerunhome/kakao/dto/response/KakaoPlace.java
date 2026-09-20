package org.zerozero.homerunhome.kakao.dto.response;

import org.zerozero.homerunhome.kakao.dto.common.KakaoCoordinate;

public record KakaoPlace(
        String id,
        String name,
        String categoryName,
        String categoryGroupCode,
        String phone,
        String addressName,
        String roadAddressName,
        KakaoCoordinate coordinate,
        String placeUrl,
        Integer distanceMeters) {}
