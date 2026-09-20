package org.zerozero.homerunhome.place.dto;

import java.math.BigDecimal;

public record PlaceResponse(
        String id,
        String name,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        PlaceCategory category,
        String categoryName,
        String phone,
        String placeUrl,
        Integer distanceMeters) {}
