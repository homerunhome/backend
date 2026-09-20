package org.zerozero.homerunhome.kakao.dto.request;

public record KakaoAddressSearchRequest(String query, int page, int size) {

    public KakaoAddressSearchRequest {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        if (page < 1 || page > 45) {
            throw new IllegalArgumentException("page must be between 1 and 45");
        }
        if (size < 1 || size > 30) {
            throw new IllegalArgumentException("size must be between 1 and 30");
        }
    }

    public static KakaoAddressSearchRequest firstPage(String query) {
        return new KakaoAddressSearchRequest(query, 1, 10);
    }
}
