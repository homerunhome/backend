package org.zerozero.homerunhome.place.dto;

public record PlaceSearchRequest(
        String keyword,
        PlaceCategory category,
        MapBounds bounds,
        Integer page,
        Integer size) {}
