package org.zerozero.homerunhome.train.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "TAGO 열차역")
public record TrainStationResponse(
        @Schema(description = "역 코드", example = "NAT010000") String stationId,
        @Schema(description = "역명", example = "서울") String stationName,
        @Schema(description = "도시 코드", example = "11") String cityCode) {}
