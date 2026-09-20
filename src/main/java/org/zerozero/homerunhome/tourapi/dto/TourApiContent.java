package org.zerozero.homerunhome.tourapi.dto;

import java.math.BigDecimal;

public record TourApiContent(
        String contentId,
        String contentTypeId,
        String title,
        String address,
        String addressDetail,
        String phone,
        String homepage,
        String overview,
        String imageUrl,
        String thumbnailUrl,
        BigDecimal longitude,
        BigDecimal latitude,
        String eventStartDate,
        String eventEndDate) {}
