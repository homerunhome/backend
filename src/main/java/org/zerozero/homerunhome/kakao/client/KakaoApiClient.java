package org.zerozero.homerunhome.kakao.client;

import org.springframework.stereotype.Component;
import org.zerozero.homerunhome.kakao.client.local.KakaoLocalClient;
import org.zerozero.homerunhome.kakao.client.mobility.KakaoMobilityClient;
import org.zerozero.homerunhome.kakao.client.routing.KakaoMapRoutingClient;
import org.zerozero.homerunhome.kakao.dto.common.KakaoSearchResult;
import org.zerozero.homerunhome.kakao.dto.request.KakaoAddressSearchRequest;
import org.zerozero.homerunhome.kakao.dto.request.KakaoCategoryPlaceSearchRequest;
import org.zerozero.homerunhome.kakao.dto.request.KakaoPlaceSearchRequest;
import org.zerozero.homerunhome.kakao.dto.request.KakaoRouteRequest;
import org.zerozero.homerunhome.kakao.dto.response.KakaoAddress;
import org.zerozero.homerunhome.kakao.dto.response.KakaoPlace;
import org.zerozero.homerunhome.kakao.dto.response.KakaoRoute;
import org.zerozero.homerunhome.kakao.dto.response.KakaoTravelRoute;

@Component
public class KakaoApiClient {

    private final KakaoLocalClient localClient;
    private final KakaoMobilityClient mobilityClient;
    private final KakaoMapRoutingClient mapRoutingClient;

    KakaoApiClient(
            KakaoLocalClient localClient,
            KakaoMobilityClient mobilityClient,
            KakaoMapRoutingClient mapRoutingClient) {
        this.localClient = localClient;
        this.mobilityClient = mobilityClient;
        this.mapRoutingClient = mapRoutingClient;
    }

    public KakaoSearchResult<KakaoAddress> searchAddresses(KakaoAddressSearchRequest request) {
        return localClient.searchAddresses(request);
    }

    public KakaoSearchResult<KakaoPlace> searchPlaces(KakaoPlaceSearchRequest request) {
        return localClient.searchPlaces(request);
    }

    public KakaoSearchResult<KakaoPlace> searchPlacesByCategory(KakaoCategoryPlaceSearchRequest request) {
        return localClient.searchPlacesByCategory(request);
    }

    public KakaoRoute findDrivingRoute(KakaoRouteRequest request) {
        return mobilityClient.findDrivingRoute(request);
    }

    public KakaoTravelRoute findWalkingRoute(KakaoRouteRequest request) {
        return mapRoutingClient.findWalkingRoute(request);
    }

    public KakaoTravelRoute findPublicTransitRoute(KakaoRouteRequest request) {
        return mapRoutingClient.findPublicTransitRoute(request);
    }
}
