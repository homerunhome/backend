package org.zerozero.homerunhome.itinerary.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.constraints.Min;

@Schema(description = "선택한 경기로 원정 일정 생성 요청")
public record ItineraryRequest(
        @Schema(description = "경기 식별자", example = "game-2026-09-20-001")
        @NotBlank @Size(max = 100) String gameId,
        @Schema(description = "도착 장소", example = "서울역")
        @NotBlank @Size(max = 200) String arrivalPlace,
        @Schema(description = "도착 일시", example = "2026-09-20T12:00:00")
        @NotNull LocalDateTime arrivalAt,
        @Valid ItineraryTrainRequest arrivalTrain,
        @Schema(description = "출발 장소", example = "서울역")
        @NotBlank @Size(max = 200) String departurePlace,
        @Schema(description = "출발 일시", example = "2026-09-20T23:30:00")
        @NotNull LocalDateTime departureAt,
        @Valid ItineraryTrainRequest returnTrain,
        @Schema(description = "선택한 관광 성향", example = "[\"FOOD\", \"CAFE\"]")
        @Size(max = 10) List<@NotBlank @Size(max = 50) String> preferences,
        @Schema(description = "일정 내 장소 목록. 전달한 배열 순서대로 저장됩니다.")
        @Size(max = 100) List<@Valid PlaceRequest> places,
        @Size(max = 30) String travelMode,
        @Min(0) Integer stadiumEntryBufferMinutes,
        LocalDateTime stadiumArrivalAt,
        Long gameStartSlackMinutes,
        @Size(max = 40) String status,
        @Size(max = 20) List<@NotBlank @Size(max = 50) String> warnings,
        @Size(max = 300) List<@Valid TimelineRequest> timeline) {

    @Schema(description = "일정에 포함할 장소")
    public record PlaceRequest(
            @Schema(description = "외부 장소 식별자", example = "kakao-place-id")
            @Size(max = 100) String placeId,
            @Schema(description = "장소명", example = "성심당")
            @NotBlank @Size(max = 200) String name,
            @Schema(description = "주소", example = "대전광역시 중구 대종로480번길 15")
            @Size(max = 500) String address,
            @Schema(description = "위도", example = "36.3275")
            BigDecimal latitude,
            @Schema(description = "경도", example = "127.4270")
            BigDecimal longitude,
            @Min(0) Long travelFromPreviousMinutes,
            LocalDateTime arrivesAt,
            LocalDateTime departsAt,
            @Min(0) Long stayDurationMinutes,
            Boolean mustVisit,
            Boolean scheduled) {}

    @Schema(description = "코스 시간표 항목")
    public record TimelineRequest(
            @NotBlank @Size(max = 40) String type,
            @NotBlank @Size(max = 300) String description,
            @NotNull LocalDateTime startsAt,
            @NotNull LocalDateTime endsAt) {}
}
