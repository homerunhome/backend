package org.zerozero.homerunhome.travel.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerozero.homerunhome.common.ApiErrorResponse;
import org.zerozero.homerunhome.travel.dto.TravelTimeRequest;
import org.zerozero.homerunhome.travel.dto.TravelTimeResponse;
import org.zerozero.homerunhome.travel.service.TravelTimeService;

@RestController
@RequestMapping("/api/travel-times")
@RequiredArgsConstructor
@Tag(name = "이동시간", description = "장소 간 이동시간과 거리 조회")
public class TravelTimeController {

    private final TravelTimeService travelTimeService;

    @PostMapping
    @Operation(summary = "장소 간 이동시간 조회", description = "현재 자동차 이동만 지원합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "이동시간 조회 성공"),
        @ApiResponse(
                responseCode = "400",
                description = "요청 조건 오류 또는 미지원 이동수단",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "경로 없음",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "429",
                description = "카카오 API 호출 한도 초과",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "502",
                description = "카카오 API 호출 실패",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiErrorResponse.class)))
    })
    public TravelTimeResponse findTravelTime(@RequestBody TravelTimeRequest request) {
        return travelTimeService.findTravelTime(request);
    }
}
