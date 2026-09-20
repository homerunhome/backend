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
import org.zerozero.homerunhome.itinerary.domain.ItineraryTimelineEntry;
import org.zerozero.homerunhome.itinerary.domain.ItineraryTrain;

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
        TrainResponse arrivalTrain,
        String departurePlace,
        LocalDateTime departureAt,
        TrainResponse returnTrain,
        List<String> preferences,
        List<PlaceResponse> places,
        String travelMode,
        Integer stadiumEntryBufferMinutes,
        LocalDateTime stadiumArrivalAt,
        Long gameStartSlackMinutes,
        LocalDateTime stadiumDepartureRecommendedAt,
        Boolean returnTransportBoardable,
        LocalDateTime expectedGameEndAt,
        Boolean gameEndMayBeDelayed,
        String status,
        List<String> warnings,
        List<TimelineResponse> timeline,
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
                TrainResponse.from(itinerary.getArrivalTrain()),
                itinerary.getDeparturePlace(),
                itinerary.getDepartureAt(),
                TrainResponse.from(itinerary.getReturnTrain()),
                itinerary.getPreferences(),
                itinerary.getPlaces().stream()
                        .map(place -> new PlaceResponse(
                                place.getPlaceId(),
                                place.getName(),
                                place.getAddress(),
                                place.getLatitude(),
                                place.getLongitude(),
                                place.getTravelFromPreviousMinutes(),
                                place.getArrivesAt(),
                                place.getDepartsAt(),
                                place.getStayDurationMinutes(),
                                place.getMustVisit(),
                                place.getScheduled()))
                        .toList(),
                itinerary.getTravelMode(),
                itinerary.getStadiumEntryBufferMinutes(),
                itinerary.getStadiumArrivalAt(),
                itinerary.getGameStartSlackMinutes(),
                itinerary.getStadiumDepartureRecommendedAt(),
                itinerary.getExpectedGameEndAt() == null || itinerary.getStadiumDepartureRecommendedAt() == null
                        ? null
                        : !itinerary.getExpectedGameEndAt().isAfter(itinerary.getStadiumDepartureRecommendedAt()),
                itinerary.getExpectedGameEndAt(),
                itinerary.getGameEndMayBeDelayed(),
                itinerary.getPlanStatus(),
                itinerary.getPlanWarnings(),
                itinerary.getTimeline().stream()
                        .map(entry -> new TimelineResponse(
                                entry.getType(), entry.getDescription(), entry.getStartsAt(), entry.getEndsAt()))
                        .toList(),
                itinerary.getCreatedAt());
    }

    @Schema(description = "일정 내 장소 응답")
    public record PlaceResponse(
            String placeId,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            Long travelFromPreviousMinutes,
            LocalDateTime arrivesAt,
            LocalDateTime departsAt,
            Long stayDurationMinutes,
            Boolean mustVisit,
            Boolean scheduled) {}

    public record TimelineResponse(String type, String description, LocalDateTime startsAt, LocalDateTime endsAt) {}

    @Schema(description = "일정에서 선택한 열차")
    public record TrainResponse(
            String trainNumber,
            String trainType,
            String departureStation,
            LocalDateTime departureAt,
            String arrivalStation,
            LocalDateTime arrivalAt) {

        private static TrainResponse from(ItineraryTrain train) {
            return train == null
                    ? null
                    : new TrainResponse(
                            train.getTrainNumber(),
                            train.getTrainType(),
                            train.getDepartureStation(),
                            train.getDepartureAt(),
                            train.getArrivalStation(),
                            train.getArrivalAt());
        }
    }
}
