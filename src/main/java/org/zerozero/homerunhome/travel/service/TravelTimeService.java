package org.zerozero.homerunhome.travel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
import org.zerozero.homerunhome.travel.dto.TravelTimeResponse;

@Service
@RequiredArgsConstructor
public class TravelTimeService {

    private final KakaoApiClient kakaoApiClient;

    public TravelTimeResponse findTravelTime(TravelTimeRequest request) {
        validateRequest(request);
        KakaoRouteRequest routeRequest = new KakaoRouteRequest(
                toKakaoCoordinate(request.origin()),
                toKakaoCoordinate(request.destination()));
        return switch (request.transportMode()) {
            case CAR -> drivingResponse(kakaoApiClient.findDrivingRoute(routeRequest));
            case WALK -> mapRouteResponse(TravelMode.WALK, kakaoApiClient.findWalkingRoute(routeRequest));
            case PUBLIC_TRANSIT -> mapRouteResponse(
                    TravelMode.PUBLIC_TRANSIT, kakaoApiClient.findPublicTransitRoute(routeRequest));
            case BICYCLE -> throw new BusinessException(ErrorCode.UNSUPPORTED_TRAVEL_MODE);
        };
    }

    private TravelTimeResponse drivingResponse(KakaoRoute route) {
        return new TravelTimeResponse(TravelMode.CAR, route.duration().toSeconds(), route.distanceMeters());
    }

    private TravelTimeResponse mapRouteResponse(TravelMode mode, KakaoTravelRoute route) {
        return new TravelTimeResponse(mode, route.duration().toSeconds(), route.distanceMeters());
    }

    private void validateRequest(TravelTimeRequest request) {
        if (request == null
                || request.origin() == null
                || request.destination() == null
                || request.transportMode() == null) {
            throw invalidRequest();
        }
    }

    private KakaoCoordinate toKakaoCoordinate(TravelCoordinate coordinate) {
        try {
            return new KakaoCoordinate(coordinate.longitude(), coordinate.latitude());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw invalidRequest();
        }
    }

    private BusinessException invalidRequest() {
        return new BusinessException(ErrorCode.INVALID_TRAVEL_TIME_REQUEST);
    }
}
