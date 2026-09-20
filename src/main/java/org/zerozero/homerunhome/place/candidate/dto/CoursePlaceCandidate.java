package org.zerozero.homerunhome.place.candidate.dto;

import java.math.BigDecimal;

public record CoursePlaceCandidate(
        CoursePlaceProvider provider,
        String externalId,
        String name,
        CoursePlaceCategory category,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String imageUrl,
        String detailUrl,
        Integer distanceMeters) {}
