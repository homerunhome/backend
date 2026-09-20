package org.zerozero.homerunhome.tourism.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TourismEventResponse(
        String contentId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String address,
        String addressDetail,
        BigDecimal latitude,
        BigDecimal longitude,
        String phone,
        String imageUrl,
        String thumbnailUrl) {}
