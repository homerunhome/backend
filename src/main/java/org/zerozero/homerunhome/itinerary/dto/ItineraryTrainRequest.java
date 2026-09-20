package org.zerozero.homerunhome.itinerary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "일정에서 선택한 열차 시간표")
public record ItineraryTrainRequest(
        @NotBlank @Size(max = 20) String trainNumber,
        @NotBlank @Size(max = 30) String trainType,
        @NotBlank @Size(max = 100) String departureStation,
        @NotNull LocalDateTime departureAt,
        @NotBlank @Size(max = 100) String arrivalStation,
        @NotNull LocalDateTime arrivalAt) {}
