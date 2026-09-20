package org.zerozero.homerunhome.itinerary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "저장된 일정의 장소와 경로시간 전체 교체 요청")
public record ItineraryPlacesEditRequest(
        @NotNull @Min(0) @Max(1440) Integer stadiumEntryBufferMinutes,
        @NotNull @Min(0) @Max(1440) Long finalLegToStadiumMinutes,
        @NotNull @Min(0) @Max(1440) Long stadiumToDepartureMinutes,
        @NotNull @Min(0) @Max(1440) Integer postGameCrowdBufferMinutes,
        @NotNull @Min(0) @Max(1440) Integer boardingBufferMinutes,
        @Min(1) @Max(1440) Integer expectedGameDurationMinutes,
        @NotNull @Size(max = 30) List<@NotNull @Valid PlaceEditRequest> places) {

    @Schema(description = "장소 후보 응답과 이동시간 API 결과를 반영한 방문 항목")
    public record PlaceEditRequest(
            @NotBlank @Size(max = 100) String placeId,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 500) String address,
            @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
            @NotNull @Min(0) @Max(1440) Long travelFromPreviousMinutes,
            @NotNull @Min(0) @Max(1440) Long stayDurationMinutes,
            Boolean mustVisit) {}
}
