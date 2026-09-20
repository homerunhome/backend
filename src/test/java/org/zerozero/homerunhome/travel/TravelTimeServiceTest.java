package org.zerozero.homerunhome.travel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.kakao.client.KakaoApiClient;
import org.zerozero.homerunhome.kakao.dto.common.KakaoCoordinate;
import org.zerozero.homerunhome.kakao.dto.request.KakaoRouteRequest;
import org.zerozero.homerunhome.kakao.dto.response.KakaoRoute;
import org.zerozero.homerunhome.kakao.dto.response.KakaoTravelRoute;
import org.zerozero.homerunhome.travel.dto.TravelCoordinate;
import org.zerozero.homerunhome.travel.dto.TravelMode;
import org.zerozero.homerunhome.travel.dto.TravelTimeRequest;
import org.zerozero.homerunhome.travel.service.TravelTimeService;

class TravelTimeServiceTest {

    private KakaoApiClient kakaoApiClient;
    private TravelTimeService service;

    @BeforeEach
    void setUp() {
        kakaoApiClient = mock(KakaoApiClient.class);
        service = new TravelTimeService(kakaoApiClient);
    }

    @Test
    void returnsDrivingDurationInSecondsAndDistanceInMeters() {
        KakaoRouteRequest kakaoRequest = new KakaoRouteRequest(
                new KakaoCoordinate(new BigDecimal("127.0719"), new BigDecimal("37.5121")),
                new KakaoCoordinate(new BigDecimal("127.0276"), new BigDecimal("37.4982")));
        when(kakaoApiClient.findDrivingRoute(kakaoRequest))
                .thenReturn(new KakaoRoute(
                        KakaoRoute.TravelMode.CAR,
                        9400,
                        Duration.ofSeconds(1280),
                        15000,
                        0));

        var response = service.findTravelTime(new TravelTimeRequest(
                coordinate("37.5121", "127.0719"),
                coordinate("37.4982", "127.0276"),
                TravelMode.CAR));

        assertEquals(TravelMode.CAR, response.transportMode());
        assertEquals(1280, response.durationSeconds());
        assertEquals(9400, response.distanceMeters());
        verify(kakaoApiClient).findDrivingRoute(kakaoRequest);
    }

    @Test
    void returnsPublicTransitDurationAndDistance() {
        KakaoRouteRequest kakaoRequest = new KakaoRouteRequest(
                new KakaoCoordinate(new BigDecimal("127.0719"), new BigDecimal("37.5121")),
                new KakaoCoordinate(new BigDecimal("127.0276"), new BigDecimal("37.4982")));
        when(kakaoApiClient.findPublicTransitRoute(kakaoRequest))
                .thenReturn(new KakaoTravelRoute(5013, Duration.ofSeconds(2115)));
        TravelTimeRequest request = new TravelTimeRequest(
                coordinate("37.5121", "127.0719"),
                coordinate("37.4982", "127.0276"),
                TravelMode.PUBLIC_TRANSIT);

        var response = service.findTravelTime(request);

        assertEquals(TravelMode.PUBLIC_TRANSIT, response.transportMode());
        assertEquals(2115, response.durationSeconds());
        assertEquals(5013, response.distanceMeters());
        verify(kakaoApiClient).findPublicTransitRoute(kakaoRequest);
        verify(kakaoApiClient, never()).findDrivingRoute(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsWalkingDurationAndDistance() {
        KakaoRouteRequest kakaoRequest = new KakaoRouteRequest(
                new KakaoCoordinate(new BigDecimal("127.0719"), new BigDecimal("37.5121")),
                new KakaoCoordinate(new BigDecimal("127.0276"), new BigDecimal("37.4982")));
        when(kakaoApiClient.findWalkingRoute(kakaoRequest))
                .thenReturn(new KakaoTravelRoute(4025, Duration.ofSeconds(3914)));

        var response = service.findTravelTime(new TravelTimeRequest(
                coordinate("37.5121", "127.0719"),
                coordinate("37.4982", "127.0276"),
                TravelMode.WALK));

        assertEquals(TravelMode.WALK, response.transportMode());
        assertEquals(3914, response.durationSeconds());
        assertEquals(4025, response.distanceMeters());
        verify(kakaoApiClient).findWalkingRoute(kakaoRequest);
    }

    @Test
    void rejectsUnsupportedTravelModeWithoutCallingKakao() {
        TravelTimeRequest request = new TravelTimeRequest(
                coordinate("37.5121", "127.0719"),
                coordinate("37.4982", "127.0276"),
                TravelMode.BICYCLE);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.findTravelTime(request));

        assertEquals(ErrorCode.UNSUPPORTED_TRAVEL_MODE, exception.getErrorCode());
        verify(kakaoApiClient, never()).findDrivingRoute(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsCoordinateOutsideWgs84Range() {
        TravelTimeRequest request = new TravelTimeRequest(
                coordinate("91", "127.0719"),
                coordinate("37.4982", "127.0276"),
                TravelMode.CAR);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.findTravelTime(request));

        assertEquals(ErrorCode.INVALID_TRAVEL_TIME_REQUEST, exception.getErrorCode());
    }

    @Test
    void rejectsRequestWithMissingCoordinate() {
        TravelTimeRequest request = new TravelTimeRequest(
                new TravelCoordinate(null, new BigDecimal("127.0719")),
                coordinate("37.4982", "127.0276"),
                TravelMode.CAR);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.findTravelTime(request));

        assertEquals(ErrorCode.INVALID_TRAVEL_TIME_REQUEST, exception.getErrorCode());
    }

    @Test
    void keepsRouteNotFoundDistinctFromExternalApiFailure() {
        when(kakaoApiClient.findDrivingRoute(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.ROUTE_NOT_FOUND));
        TravelTimeRequest request = new TravelTimeRequest(
                coordinate("37.5121", "127.0719"),
                coordinate("37.4982", "127.0276"),
                TravelMode.CAR);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.findTravelTime(request));

        assertEquals(ErrorCode.ROUTE_NOT_FOUND, exception.getErrorCode());
    }

    private TravelCoordinate coordinate(String latitude, String longitude) {
        return new TravelCoordinate(new BigDecimal(latitude), new BigDecimal(longitude));
    }
}
