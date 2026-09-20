package org.zerozero.homerunhome.place.recommendation.controller;

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
import org.zerozero.homerunhome.place.recommendation.dto.PlaceRecommendationRequest;
import org.zerozero.homerunhome.place.recommendation.dto.PlaceRecommendationResponse;
import org.zerozero.homerunhome.place.recommendation.service.PlaceRecommendationService;

@RestController
@RequestMapping("/api/places/recommendations")
@RequiredArgsConstructor
@Tag(name = "장소 추천", description = "사용자 선호와 코스 범위를 반영한 장소 후보 추천")
public class PlaceRecommendationController {

    private final PlaceRecommendationService placeRecommendationService;

    @PostMapping
    @Operation(summary = "선호 카테고리와 코스 조건별 장소 추천")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "장소 추천 성공"),
        @ApiResponse(
                responseCode = "400",
                description = "추천 조건 오류",
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
    public PlaceRecommendationResponse recommend(@RequestBody PlaceRecommendationRequest request) {
        return placeRecommendationService.recommend(request);
    }
}
