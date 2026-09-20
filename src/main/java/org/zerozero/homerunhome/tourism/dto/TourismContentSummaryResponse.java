package org.zerozero.homerunhome.tourism.dto;

import java.math.BigDecimal;

public record TourismContentSummaryResponse(
        String contentId,
        TourismContentType contentType,
        String title,
        String address,
        String addressDetail,
        BigDecimal latitude,
        BigDecimal longitude,
        String phone,
        String imageUrl,
        String thumbnailUrl) {}
