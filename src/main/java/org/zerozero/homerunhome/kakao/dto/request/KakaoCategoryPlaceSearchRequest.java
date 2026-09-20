package org.zerozero.homerunhome.kakao.dto.request;

import org.zerozero.homerunhome.kakao.dto.common.KakaoCoordinate;
import org.zerozero.homerunhome.kakao.dto.common.KakaoMapBounds;

public record KakaoCategoryPlaceSearchRequest(
        String categoryGroupCode,
        KakaoCoordinate center,
        Integer radiusMeters,
        KakaoMapBounds bounds,
        int page,
        int size) {

    public KakaoCategoryPlaceSearchRequest {
        if (categoryGroupCode == null || categoryGroupCode.isBlank()) {
            throw new IllegalArgumentException("categoryGroupCode must not be blank");
        }
        if (bounds == null && center == null) {
            throw new IllegalArgumentException("bounds or center must not be null");
        }
        if (bounds != null && (center != null || radiusMeters != null)) {
            throw new IllegalArgumentException("bounds cannot be combined with center or radiusMeters");
        }
        if (center != null && radiusMeters == null) {
            throw new IllegalArgumentException("radiusMeters is required when center is set");
        }
        if (radiusMeters != null && (radiusMeters < 1 || radiusMeters > 20_000)) {
            throw new IllegalArgumentException("radiusMeters must be between 1 and 20000");
        }
        if (page < 1 || page > 45) {
            throw new IllegalArgumentException("page must be between 1 and 45");
        }
        if (size < 1 || size > 15) {
            throw new IllegalArgumentException("size must be between 1 and 15");
        }
        categoryGroupCode = categoryGroupCode.trim();
    }

    public KakaoCategoryPlaceSearchRequest(
            String categoryGroupCode,
            KakaoMapBounds bounds,
            int page,
            int size) {
        this(categoryGroupCode, null, null, bounds, page, size);
    }

    public static KakaoCategoryPlaceSearchRequest around(
            String categoryGroupCode,
            KakaoCoordinate center,
            int radiusMeters,
            int page,
            int size) {
        return new KakaoCategoryPlaceSearchRequest(
                categoryGroupCode, center, radiusMeters, null, page, size);
    }
}
