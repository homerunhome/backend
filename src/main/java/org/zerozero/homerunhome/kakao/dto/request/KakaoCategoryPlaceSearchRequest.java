package org.zerozero.homerunhome.kakao.dto.request;

import org.zerozero.homerunhome.kakao.dto.common.KakaoMapBounds;

public record KakaoCategoryPlaceSearchRequest(
        String categoryGroupCode,
        KakaoMapBounds bounds,
        int page,
        int size) {

    public KakaoCategoryPlaceSearchRequest {
        if (categoryGroupCode == null || categoryGroupCode.isBlank()) {
            throw new IllegalArgumentException("categoryGroupCode must not be blank");
        }
        if (bounds == null) {
            throw new IllegalArgumentException("bounds must not be null");
        }
        if (page < 1 || page > 45) {
            throw new IllegalArgumentException("page must be between 1 and 45");
        }
        if (size < 1 || size > 15) {
            throw new IllegalArgumentException("size must be between 1 and 15");
        }
        categoryGroupCode = categoryGroupCode.trim();
    }
}
