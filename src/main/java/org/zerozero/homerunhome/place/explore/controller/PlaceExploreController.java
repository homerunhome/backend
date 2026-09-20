package org.zerozero.homerunhome.place.explore.controller;

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
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreRequest;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreResponse;
import org.zerozero.homerunhome.place.explore.service.PlaceExploreService;

@RestController
@RequestMapping("/api/places/explore")
@RequiredArgsConstructor
@Tag(name = "장소 탐색", description = "코스·야구장·대전역·현재 위치·대전 명소 주변 장소 탐색")
public class PlaceExploreController {

    private final PlaceExploreService placeExploreService;

    @PostMapping
    @Operation(summary = "탐색 위치와 장소 종류별 장소 조회")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "장소 탐색 성공"),
        @ApiResponse(
                responseCode = "400",
                description = "탐색 조건 오류",
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
    public PlaceExploreResponse explore(@RequestBody PlaceExploreRequest request) {
        return placeExploreService.explore(request);
    }
}
