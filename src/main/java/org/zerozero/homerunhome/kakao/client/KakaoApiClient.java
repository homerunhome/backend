package org.zerozero.homerunhome.kakao.client;

import org.springframework.stereotype.Component;
import org.zerozero.homerunhome.kakao.client.local.KakaoLocalClient;
import org.zerozero.homerunhome.kakao.client.mobility.KakaoMobilityClient;
import org.zerozero.homerunhome.kakao.dto.common.KakaoSearchResult;
import org.zerozero.homerunhome.kakao.dto.request.KakaoAddressSearchRequest;
import org.zerozero.homerunhome.kakao.dto.request.KakaoPlaceSearchRequest;
import org.zerozero.homerunhome.kakao.dto.request.KakaoRouteRequest;
import org.zerozero.homerunhome.kakao.dto.response.KakaoAddress;
import org.zerozero.homerunhome.kakao.dto.response.KakaoPlace;
import org.zerozero.homerunhome.kakao.dto.response.KakaoRoute;

@Component
public class KakaoApiClient {

    private final KakaoLocalClient localClient;
    private final KakaoMobilityClient mobilityClient;

    KakaoApiClient(KakaoLocalClient localClient, KakaoMobilityClient mobilityClient) {
        this.localClient = localClient;
        this.mobilityClient = mobilityClient;
    }

    public KakaoSearchResult<KakaoAddress> searchAddresses(KakaoAddressSearchRequest request) {
        return localClient.searchAddresses(request);
    }

    public KakaoSearchResult<KakaoPlace> searchPlaces(KakaoPlaceSearchRequest request) {
        return localClient.searchPlaces(request);
    }

    public KakaoRoute findDrivingRoute(KakaoRouteRequest request) {
        return mobilityClient.findDrivingRoute(request);
    }
}
