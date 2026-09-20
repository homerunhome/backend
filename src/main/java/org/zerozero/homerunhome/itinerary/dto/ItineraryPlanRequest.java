package org.zerozero.homerunhome.itinerary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "경기 전 방문 장소와 교통 여유를 반영한 원정 코스 생성 요청")
public record ItineraryPlanRequest(
        @NotBlank @Size(max = 100) String gameId,
        @NotBlank @Size(max = 200) String arrivalPlace,
        @NotNull LocalDateTime arrivalAt,
        @NotBlank @Size(max = 200) String departurePlace,
        @NotNull LocalDateTime departureAt,
        @NotNull @Min(0) @Max(1440) Integer stadiumEntryBufferMinutes,
        @NotNull @Min(0) @Max(1440) Long finalLegToStadiumMinutes,
        @NotNull @Min(0) @Max(1440) Long stadiumToDepartureMinutes,
        @NotNull @Min(0) @Max(1440) Integer postGameCrowdBufferMinutes,
        @NotNull @Min(0) @Max(1440) Integer boardingBufferMinutes,
        @Min(1) @Max(1440) Integer expectedGameDurationMinutes,
        @Size(max = 30) List<@NotNull @Valid PlacePlanRequest> places) {

    @Schema(description = "방문 후보. 입력 순서대로 이동시간을 누적해 일정을 생성합니다.")
    public record PlacePlanRequest(
            @Size(max = 100) String placeId,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 500) String address,
            BigDecimal latitude,
            BigDecimal longitude,
            @NotNull @Min(0) @Max(1440) Long travelFromPreviousMinutes,
            @NotNull @Min(0) @Max(1440) Long stayDurationMinutes,
            Boolean mustVisit) {}
}
