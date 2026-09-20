package org.zerozero.homerunhome.train.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zerozero.homerunhome.train.dto.TrainScheduleResponse;
import org.zerozero.homerunhome.train.dto.TrainCityResponse;
import org.zerozero.homerunhome.train.dto.TrainStationResponse;
import org.zerozero.homerunhome.train.service.TrainScheduleService;

@Validated
@RestController
@RequestMapping("/api/trains")
@RequiredArgsConstructor
@Tag(name = "열차", description = "TAGO 열차 시간표 조회")
public class TrainScheduleController {

    private final TrainScheduleService trainScheduleService;

    @GetMapping("/cities")
    @Operation(summary = "열차 운행 도시 조회", description = "TAGO에서 제공하는 열차 운행 도시 코드를 조회합니다.")
    public List<TrainCityResponse> findCities() {
        return trainScheduleService.findCities();
    }

    @GetMapping("/stations")
    @Operation(summary = "도시별 열차역 조회", description = "도시 코드에 해당하는 역 목록을 조회합니다.")
    public List<TrainStationResponse> findStations(
            @Parameter(description = "TAGO 도시 코드", example = "11")
                    @RequestParam("cityCode") @NotBlank String cityCode) {
        return trainScheduleService.findStations(cityCode);
    }

    @GetMapping("/to-daejeon")
    @Operation(summary = "대전행 전체 열차 조회", description = "선택한 출발역에서 대전의 모든 역으로 가는 열차 시간표를 조회합니다.")
    public List<TrainScheduleResponse> findSchedulesToDaejeon(
            @Parameter(description = "TAGO 출발역 코드", example = "NAT010000")
                    @RequestParam("departureStationId") @NotBlank String departureStationId,
            @Parameter(description = "운행 날짜", example = "2026-09-20")
                    @RequestParam("date") @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return trainScheduleService.findSchedulesToDaejeon(departureStationId, date);
    }

    @GetMapping("/from-daejeon")
    @Operation(summary = "대전 출발 전체 열차 조회", description = "대전의 모든 역에서 선택한 도착역으로 가는 열차 시간표를 조회합니다.")
    public List<TrainScheduleResponse> findSchedulesFromDaejeon(
            @Parameter(description = "TAGO 도착역 코드", example = "NAT010000")
                    @RequestParam("arrivalStationId") @NotBlank String arrivalStationId,
            @Parameter(description = "운행 날짜", example = "2026-09-20")
                    @RequestParam("date") @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return trainScheduleService.findSchedulesFromDaejeon(arrivalStationId, date);
    }

    @GetMapping("/ktx")
    @Operation(
            summary = "KTX 시간표 조회",
            description = "출발역·도착역 코드와 날짜로 KTX 시간표를 조회합니다. 귀가편은 출발역과 도착역을 바꿔 요청합니다.")
    public List<TrainScheduleResponse> findKtxSchedules(
            @Parameter(description = "TAGO 출발역 코드", example = "NAT010000")
                    @RequestParam("departureStationId") @NotBlank String departureStationId,
            @Parameter(description = "TAGO 도착역 코드", example = "NAT011668")
                    @RequestParam("arrivalStationId") @NotBlank String arrivalStationId,
            @Parameter(description = "운행 날짜", example = "2026-09-20")
                    @RequestParam("date") @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return trainScheduleService.findKtxSchedules(departureStationId, arrivalStationId, date);
    }
}
