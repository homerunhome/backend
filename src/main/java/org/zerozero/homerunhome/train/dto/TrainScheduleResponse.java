package org.zerozero.homerunhome.train.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "KTX 운행 시간표")
public record TrainScheduleResponse(
        @Schema(description = "열차 번호", example = "001") String trainNumber,
        @Schema(description = "열차 종류", example = "KTX") String trainType,
        @Schema(description = "출발역", example = "서울") String departureStation,
        @Schema(description = "출발 일시", example = "2026-09-20T08:00:00") LocalDateTime departureAt,
        @Schema(description = "도착역", example = "대전") String arrivalStation,
        @Schema(description = "도착 일시", example = "2026-09-20T09:00:00") LocalDateTime arrivalAt) {}
