package org.zerozero.homerunhome.place.dto;

import java.math.BigDecimal;

public record MapBounds(
        BigDecimal southWestLatitude,
        BigDecimal southWestLongitude,
        BigDecimal northEastLatitude,
        BigDecimal northEastLongitude) {}
