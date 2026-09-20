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
import org.zerozero.homerunhome.travel.dto.TravelMode;

@Schema(description = "실제 구간 이동시간을 기준으로 방문 순서를 계산하는 코스 생성 요청")
public record ItineraryAutoPlanRequest(
        @NotBlank @Size(max = 100) String gameId,
        @NotBlank @Size(max = 200) String arrivalPlace,
        @NotNull LocalDateTime arrivalAt,
        @Valid ItineraryTrainRequest arrivalTrain,
        @NotBlank @Size(max = 200) String departurePlace,
        @NotNull LocalDateTime departureAt,
        @Valid ItineraryTrainRequest returnTrain,
        @NotNull @Min(0) @Max(1440) Integer stadiumEntryBufferMinutes,
        @NotNull @Min(0) @Max(1440) Long stadiumToDepartureMinutes,
        @NotNull @Min(0) @Max(1440) Integer postGameCrowdBufferMinutes,
        @NotNull @Min(0) @Max(1440) Integer boardingBufferMinutes,
        @Min(1) @Max(1440) Integer expectedGameDurationMinutes,
        @Size(max = 10) List<@NotBlank @Size(max = 50) String> preferences,
        @NotNull TravelMode travelMode,
        @Size(max = 8) List<@NotNull @Valid PlaceCandidate> places,
        @NotNull @Size(max = 72) List<@NotNull @Valid RouteTime> travelTimes) {

    public static final String ARRIVAL_NODE_ID = "ARRIVAL";
    public static final String STADIUM_NODE_ID = "STADIUM";

    @Schema(description = "방문 순서를 정할 장소 후보. 요청한 모든 후보를 방문합니다.")
    public record PlaceCandidate(
            @NotBlank @Size(max = 100) String placeId,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 500) String address,
            BigDecimal latitude,
            BigDecimal longitude,
            @NotNull @Min(0) @Max(1440) Long stayDurationMinutes,
            Boolean mustVisit) {}

    @Schema(description = "방향이 있는 구간별 실제 이동시간. ARRIVAL에서 시작하고 STADIUM에서 끝납니다.")
    public record RouteTime(
            @NotBlank @Size(max = 100) String fromPlaceId,
            @NotBlank @Size(max = 100) String toPlaceId,
            @NotNull @Min(0) @Max(1440) Long minutes) {}
}
