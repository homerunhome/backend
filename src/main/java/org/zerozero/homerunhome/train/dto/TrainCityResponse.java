package org.zerozero.homerunhome.train.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "TAGO 열차 운행 도시")
public record TrainCityResponse(
        @Schema(description = "도시 코드", example = "11") String cityCode,
        @Schema(description = "도시명", example = "서울특별시") String cityName) {}
