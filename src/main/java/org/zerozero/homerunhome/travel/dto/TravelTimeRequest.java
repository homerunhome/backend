package org.zerozero.homerunhome.travel.dto;

public record TravelTimeRequest(
        TravelCoordinate origin,
        TravelCoordinate destination,
        TravelMode transportMode) {}
