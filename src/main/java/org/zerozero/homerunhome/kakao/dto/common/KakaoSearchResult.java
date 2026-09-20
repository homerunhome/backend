package org.zerozero.homerunhome.kakao.dto.common;

import java.util.List;

public record KakaoSearchResult<T>(List<T> items, int totalCount, int pageableCount, boolean end) {

    public KakaoSearchResult {
        items = List.copyOf(items);
    }
}
