package org.zerozero.homerunhome.place.candidate.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CoursePlaceCandidate(
        CoursePlaceProvider provider,
        String externalId,
        String name,
        CoursePlaceCategory category,
        String address,
        String phone,
        BigDecimal latitude,
        BigDecimal longitude,
        String imageUrl,
        String detailUrl,
        LocalDate eventStartDate,
        LocalDate eventEndDate,
        Integer distanceMeters) {

    public CoursePlaceCandidate(
            CoursePlaceProvider provider,
            String externalId,
            String name,
            CoursePlaceCategory category,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String imageUrl,
            String detailUrl,
            Integer distanceMeters) {
        this(
                provider,
                externalId,
                name,
                category,
                address,
                null,
                latitude,
                longitude,
                imageUrl,
                detailUrl,
                null,
                null,
                distanceMeters);
    }
}
