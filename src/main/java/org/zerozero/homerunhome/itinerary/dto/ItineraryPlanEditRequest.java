package org.zerozero.homerunhome.itinerary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import org.zerozero.homerunhome.itinerary.dto.ItineraryPlacesEditRequest.PlaceEditRequest;
import org.zerozero.homerunhome.travel.dto.TravelMode;

@Schema(description = "저장된 일정의 교통편·성향·장소를 수정하고 코스 시간표를 다시 계산하는 요청")
public record ItineraryPlanEditRequest(
        @NotBlank @Size(max = 200) String arrivalPlace,
        @NotNull LocalDateTime arrivalAt,
        @Valid ItineraryTrainRequest arrivalTrain,
        @NotBlank @Size(max = 200) String departurePlace,
        @NotNull LocalDateTime departureAt,
        @Valid ItineraryTrainRequest returnTrain,
        @NotNull @Size(max = 10) List<@NotBlank @Size(max = 50) String> preferences,
        @NotNull @Min(0) @Max(1440) Integer stadiumEntryBufferMinutes,
        @NotNull @Min(0) @Max(1440) Long finalLegToStadiumMinutes,
        @NotNull @Min(0) @Max(1440) Long stadiumToDepartureMinutes,
        @NotNull @Min(0) @Max(1440) Integer postGameCrowdBufferMinutes,
        @NotNull @Min(0) @Max(1440) Integer boardingBufferMinutes,
        @NotNull TravelMode travelMode,
        @NotNull @Min(1) @Max(1440) Integer expectedGameDurationMinutes,
        @NotNull @Size(max = 8) List<@NotNull @Valid PlaceEditRequest> places) {}
