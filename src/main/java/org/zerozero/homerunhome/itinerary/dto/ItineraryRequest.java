package org.zerozero.homerunhome.itinerary.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record ItineraryRequest(
        @NotBlank @Size(max = 100) String gameId,
        @NotNull LocalDate gameDate,
        @NotNull LocalTime gameStartTime,
        @NotBlank @Size(max = 150) String stadium,
        @NotBlank @Size(max = 100) String homeTeam,
        @NotBlank @Size(max = 100) String awayTeam,
        @NotBlank @Size(max = 200) String arrivalPlace,
        @NotNull LocalDateTime arrivalAt,
        @NotBlank @Size(max = 200) String departurePlace,
        @NotNull LocalDateTime departureAt,
        @Size(max = 10) List<@NotBlank @Size(max = 50) String> preferences,
        @Size(max = 100) List<@Valid PlaceRequest> places) {

    public record PlaceRequest(
            @Size(max = 100) String placeId,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 500) String address,
            BigDecimal latitude,
            BigDecimal longitude) {}
}
