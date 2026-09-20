package org.zerozero.homerunhome.itinerary.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import org.zerozero.homerunhome.game.domain.Game;
import org.zerozero.homerunhome.itinerary.domain.Itinerary;

@Schema(description = "원정 일정 응답")
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
        Game game = itinerary.getGame();
        return new ItineraryResponse(
                itinerary.getId(),
                game.getGameId(),
                game.getGameDate(),
                game.getGameStartTime(),
                game.getStadium(),
                game.getHomeTeam(),
                game.getAwayTeam(),
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

    @Schema(description = "일정 내 장소 응답")
    public record PlaceResponse(
            String placeId, String name, String address, BigDecimal latitude, BigDecimal longitude) {}
}
