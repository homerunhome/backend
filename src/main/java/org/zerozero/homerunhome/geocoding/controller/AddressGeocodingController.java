package org.zerozero.homerunhome.geocoding.controller;

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
import org.zerozero.homerunhome.geocoding.dto.AddressCoordinateResponse;
import org.zerozero.homerunhome.geocoding.dto.AddressGeocodingRequest;
import org.zerozero.homerunhome.geocoding.service.AddressGeocodingService;

@RestController
@RequestMapping("/api/geocoding")
@RequiredArgsConstructor
@Tag(name = "주소 좌표 변환", description = "도로명 또는 지번 주소를 위도·경도로 변환")
public class AddressGeocodingController {

    private final AddressGeocodingService addressGeocodingService;

    @PostMapping
    @Operation(summary = "주소 좌표 변환", description = "주소 검색 결과가 하나로 확정되면 위도와 경도를 반환합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "좌표 변환 성공"),
        @ApiResponse(
                responseCode = "400",
                description = "빈 주소 또는 잘못된 주소 형식",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "주소 검색 결과 없음",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "주소 검색 결과를 하나로 확정할 수 없음",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiErrorResponse.class)))
    })
    public AddressCoordinateResponse geocode(@RequestBody AddressGeocodingRequest request) {
        return addressGeocodingService.geocode(request == null ? null : request.address());
    }
}
