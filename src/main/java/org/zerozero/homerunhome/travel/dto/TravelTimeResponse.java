package org.zerozero.homerunhome.travel.dto;

public record TravelTimeResponse(
        TravelMode transportMode,
        long durationSeconds,
        int distanceMeters) {}
