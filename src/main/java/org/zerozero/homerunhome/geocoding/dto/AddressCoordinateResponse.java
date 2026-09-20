package org.zerozero.homerunhome.geocoding.dto;

import java.math.BigDecimal;

public record AddressCoordinateResponse(
        String addressName,
        String roadAddressName,
        BigDecimal latitude,
        BigDecimal longitude) {}
