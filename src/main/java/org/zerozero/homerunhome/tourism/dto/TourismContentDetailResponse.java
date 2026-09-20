package org.zerozero.homerunhome.tourism.dto;

import java.math.BigDecimal;
import java.util.List;

public record TourismContentDetailResponse(
        String contentId,
        TourismContentType contentType,
        String title,
        String overview,
        String address,
        String addressDetail,
        BigDecimal latitude,
        BigDecimal longitude,
        String phone,
        String homepageUrl,
        String imageUrl,
        String thumbnailUrl,
        List<TourismImageResponse> images,
        TourismAccessibilityResponse accessibility) {

    public TourismContentDetailResponse {
        images = List.copyOf(images);
    }
}
