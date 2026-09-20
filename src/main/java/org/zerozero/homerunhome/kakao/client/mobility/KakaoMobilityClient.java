package org.zerozero.homerunhome.kakao.client.mobility;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.kakao.client.support.KakaoClientExecutor;
import org.zerozero.homerunhome.kakao.dto.common.KakaoCoordinate;
import org.zerozero.homerunhome.kakao.dto.request.KakaoRouteRequest;
import org.zerozero.homerunhome.kakao.dto.response.KakaoRoute;

@Component
public class KakaoMobilityClient {

    private final RestClient restClient;
    private final KakaoClientExecutor executor;

    public KakaoMobilityClient(
            @Qualifier("kakaoMobilityRestClient") RestClient restClient,
            KakaoClientExecutor executor) {
        this.restClient = restClient;
        this.executor = executor;
    }

    public KakaoRoute findDrivingRoute(KakaoRouteRequest request) {
        return executor.execute("drivingRoute", () -> {
            KakaoRouteResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v1/directions")
                            .queryParam("origin", coordinateValue(request.origin()))
                            .queryParam("destination", coordinateValue(request.destination()))
                            .queryParam("priority", "RECOMMEND")
                            .queryParam("summary", true)
                            .build())
                    .retrieve()
                    .body(KakaoRouteResponse.class);
            return mapRoute(response);
        });
    }

    private KakaoRoute mapRoute(KakaoRouteResponse response) {
        if (response == null || response.routes() == null || response.routes().isEmpty()) {
            throw new BusinessException(ErrorCode.KAKAO_API_REQUEST_FAILED);
        }
        KakaoRouteResponse.Route route = response.routes().getFirst();
        if (route.resultCode() != 0 || route.summary() == null || route.summary().fare() == null) {
            throw new BusinessException(ErrorCode.KAKAO_API_REQUEST_FAILED);
        }
        KakaoRouteResponse.Summary summary = route.summary();
        return new KakaoRoute(
                KakaoRoute.TravelMode.CAR,
                summary.distance(),
                Duration.ofSeconds(summary.duration()),
                summary.fare().taxi(),
                summary.fare().toll());
    }

    private String coordinateValue(KakaoCoordinate coordinate) {
        return coordinate.longitude().toPlainString() + "," + coordinate.latitude().toPlainString();
    }
}
