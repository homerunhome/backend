package org.zerozero.homerunhome.itinerary.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.zerozero.homerunhome.itinerary.domain.Itinerary;

public record ItineraryResponse(
        Long id,
        String gameId,
        LocalDate gameDate,
        LocalTime gameStartTime,
        String stadium,
        String homeTeam,
        String awayTeam,
        String arrivalPlace,
        LocalDateTime arrivalAt,
        String departurePlace,
        LocalDateTime departureAt,
        List<String> preferences,
        List<PlaceResponse> places,
        Instant createdAt) {

    public static ItineraryResponse from(Itinerary itinerary) {
        return new ItineraryResponse(
                itinerary.getId(),
                itinerary.getGameId(),
                itinerary.getGameDate(),
                itinerary.getGameStartTime(),
                itinerary.getStadium(),
                itinerary.getHomeTeam(),
                itinerary.getAwayTeam(),
                itinerary.getArrivalPlace(),
                itinerary.getArrivalAt(),
                itinerary.getDeparturePlace(),
                itinerary.getDepartureAt(),
                itinerary.getPreferences(),
                itinerary.getPlaces().stream()
                        .map(place -> new PlaceResponse(
                                place.getPlaceId(),
                                place.getName(),
                                place.getAddress(),
                                place.getLatitude(),
                                place.getLongitude()))
                        .toList(),
                itinerary.getCreatedAt());
    }

    public record PlaceResponse(
            String placeId, String name, String address, BigDecimal latitude, BigDecimal longitude) {}
}
