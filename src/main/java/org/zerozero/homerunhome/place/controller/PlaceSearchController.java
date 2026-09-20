package org.zerozero.homerunhome.place.controller;

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
import org.zerozero.homerunhome.place.dto.PlaceSearchRequest;
import org.zerozero.homerunhome.place.dto.PlaceSearchResponse;
import org.zerozero.homerunhome.place.service.PlaceSearchService;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
@Tag(name = "장소 검색", description = "키워드·카테고리·지도 영역 기반 장소 검색")
public class PlaceSearchController {

    private final PlaceSearchService placeSearchService;

    @PostMapping("/search")
    @Operation(summary = "장소 검색", description = "검색 결과가 없으면 빈 장소 목록을 반환합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "장소 검색 성공"),
        @ApiResponse(
                responseCode = "400",
                description = "검색 조건 오류",
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
    public PlaceSearchResponse search(@RequestBody PlaceSearchRequest request) {
        return placeSearchService.search(request);
    }
}
