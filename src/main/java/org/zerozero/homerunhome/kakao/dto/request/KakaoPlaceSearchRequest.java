package org.zerozero.homerunhome.kakao.dto.request;

import org.zerozero.homerunhome.kakao.dto.common.KakaoCoordinate;

public record KakaoPlaceSearchRequest(
        String query,
        KakaoCoordinate center,
        Integer radiusMeters,
        KakaoPlaceSearchSort sort,
        int page,
        int size) {

    public KakaoPlaceSearchRequest {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        if (page < 1 || page > 45) {
            throw new IllegalArgumentException("page must be between 1 and 45");
        }
        if (size < 1 || size > 15) {
            throw new IllegalArgumentException("size must be between 1 and 15");
        }
        if (radiusMeters != null && (radiusMeters < 0 || radiusMeters > 20_000)) {
            throw new IllegalArgumentException("radiusMeters must be between 0 and 20000");
        }
        if (radiusMeters != null && center == null) {
            throw new IllegalArgumentException("center is required when radiusMeters is set");
        }
        if (sort == KakaoPlaceSearchSort.DISTANCE && center == null) {
            throw new IllegalArgumentException("center is required for distance sorting");
        }
        sort = sort == null ? KakaoPlaceSearchSort.ACCURACY : sort;
    }

    public static KakaoPlaceSearchRequest firstPage(String query) {
        return new KakaoPlaceSearchRequest(query, null, null, KakaoPlaceSearchSort.ACCURACY, 1, 15);
    }
}
