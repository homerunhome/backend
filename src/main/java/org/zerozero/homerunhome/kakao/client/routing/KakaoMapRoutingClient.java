package org.zerozero.homerunhome.kakao.client.routing;

import java.time.Duration;
import java.util.Comparator;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.kakao.client.support.KakaoClientExecutor;
import org.zerozero.homerunhome.kakao.dto.common.KakaoCoordinate;
import org.zerozero.homerunhome.kakao.dto.request.KakaoRouteRequest;

@Component
public class KakaoMapRoutingClient {

    private final RestClient restClient;
    private final KakaoClientExecutor executor;

    public KakaoMapRoutingClient(
            @Qualifier("kakaoLocalRestClient") RestClient restClient, KakaoClientExecutor executor) {
        this.restClient = restClient;
        this.executor = executor;
    }

    public Duration findWalkingTime(KakaoRouteRequest request) {
        return executor.execute("walkingRoute", () -> {
            KakaoWalkingRouteResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v2/routing/walk")
                            .queryParam("start_x", request.origin().longitude().toPlainString())
                            .queryParam("start_y", request.origin().latitude().toPlainString())
                            .queryParam("end_x", request.destination().longitude().toPlainString())
                            .queryParam("end_y", request.destination().latitude().toPlainString())
                            .build())
                    .retrieve()
                    .body(KakaoWalkingRouteResponse.class);
            if (response == null
                    || !"OK".equals(response.status())
                    || response.route() == null
                    || response.route().properties() == null
                    || response.route().properties().totalTime() == null
                    || response.route().properties().totalTime() < 0) {
                throw new BusinessException(ErrorCode.ROUTE_NOT_FOUND);
            }
            return Duration.ofSeconds(response.route().properties().totalTime());
        });
    }

    public Duration findPublicTransitTime(KakaoRouteRequest request) {
        return executor.execute("publicTransitRoute", () -> {
            KakaoPublicTransitResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v2/routing/publictraffic")
                            .queryParam("start_x", request.origin().longitude().toPlainString())
                            .queryParam("start_y", request.origin().latitude().toPlainString())
                            .queryParam("end_x", request.destination().longitude().toPlainString())
                            .queryParam("end_y", request.destination().latitude().toPlainString())
                            .build())
                    .retrieve()
                    .body(KakaoPublicTransitResponse.class);
            if (response == null || !"OK".equals(response.status()) || response.routes() == null) {
                throw new BusinessException(ErrorCode.ROUTE_NOT_FOUND);
            }
            return response.routes().stream()
                    .filter(Objects::nonNull)
                    .map(KakaoPublicTransitResponse.Route::properties)
                    .filter(Objects::nonNull)
                    .map(KakaoPublicTransitResponse.RouteProperties::totalTime)
                    .filter(Objects::nonNull)
                    .filter(time -> time >= 0)
                    .min(Comparator.naturalOrder())
                    .map(Duration::ofSeconds)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ROUTE_NOT_FOUND));
        });
    }
}
