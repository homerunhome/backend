package org.zerozero.homerunhome.itinerary.controller;

import jakarta.validation.Valid;
import java.net.URI;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerozero.homerunhome.common.ApiErrorResponse;
import org.zerozero.homerunhome.itinerary.dto.ItineraryAutoPlanRequest;
import org.zerozero.homerunhome.itinerary.dto.ItineraryPlanRequest;
import org.zerozero.homerunhome.itinerary.dto.ItineraryPlacesEditRequest;
import org.zerozero.homerunhome.itinerary.dto.ItineraryRequest;
import org.zerozero.homerunhome.itinerary.dto.ItineraryResponse;
import org.zerozero.homerunhome.itinerary.service.ItineraryService;

@RestController
@RequestMapping("/api/itineraries")
@RequiredArgsConstructor
@Tag(name = "원정 일정", description = "원정 일정 생성 및 조회")
public class ItineraryController {

    private final ItineraryService itineraryService;

    @PostMapping("/generate")
    @Operation(
            summary = "경기 전 기본 코스 생성",
            description = "장소별 이동·체류시간과 교통편 여유시간으로 시간순 일정을 계산하고 선택한 열차 정보를 함께 저장합니다.")
    public ResponseEntity<ItineraryResponse> generate(@Valid @RequestBody ItineraryPlanRequest request) {
        ItineraryResponse response = itineraryService.generate(request);
        return ResponseEntity.created(URI.create("/api/itineraries/" + response.id())).body(response);
    }

    @PostMapping("/generate/optimized")
    @Operation(
            summary = "실제 이동시간 기준 코스 생성",
            description = "이미 선택한 장소 후보와 구간별 이동시간을 기준으로 전체 후보를 방문하는 최단 이동 순서를 계산합니다.")
    public ResponseEntity<ItineraryResponse> generateOptimized(
            @Valid @RequestBody ItineraryAutoPlanRequest request) {
        ItineraryResponse response = itineraryService.generateOptimized(request);
        return ResponseEntity.created(URI.create("/api/itineraries/" + response.id())).body(response);
    }

    @PostMapping
    @Operation(summary = "원정 일정 생성", description = "선택한 경기, 왕편·귀가편 열차, 성향 및 장소 순서를 저장합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "일정 생성 성공"),
        @ApiResponse(
                responseCode = "400",
                description = "요청 값 검증 실패",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "선택한 경기를 찾을 수 없음",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ItineraryResponse> create(@Valid @RequestBody ItineraryRequest request) {
        ItineraryResponse response = itineraryService.create(request);
        return ResponseEntity.created(URI.create("/api/itineraries/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "원정 일정 수정", description = "저장된 일정의 장소 순서와 코스 정보를 수정합니다.")
    public ItineraryResponse update(
            @Parameter(description = "자동 증가 일정 ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody ItineraryRequest request) {
        return itineraryService.update(id, request);
    }

    @PutMapping("/{id}/places")
    @Operation(
            summary = "저장된 일정의 장소 편집",
            description = "장소 전체 목록을 요청 순서로 교체하고 장소별 이동·체류시간을 반영해 일정을 다시 계산합니다.")
    public ItineraryResponse updatePlaces(
            @Parameter(description = "자동 증가 일정 ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody ItineraryPlacesEditRequest request) {
        return itineraryService.updatePlaces(id, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "원정 일정 상세 조회", description = "저장된 일정과 순서가 있는 장소 목록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "일정 조회 성공"),
        @ApiResponse(
                responseCode = "400",
                description = "일정 ID 형식 오류",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "일정을 찾을 수 없음",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiErrorResponse.class)))
    })
    public ItineraryResponse get(@Parameter(description = "자동 증가 일정 ID", example = "1") @PathVariable Long id) {
        return itineraryService.get(id);
    }
}
