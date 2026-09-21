package org.zerozero.homerunhome.kakao.client.routing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.kakao.client.support.KakaoClientExecutor;
import org.zerozero.homerunhome.kakao.dto.common.KakaoCoordinate;
import org.zerozero.homerunhome.kakao.dto.request.KakaoRouteRequest;
import org.zerozero.homerunhome.kakao.dto.response.KakaoTravelRoute;

@Component
public class KakaoMapRoutingClient {

    private static final Logger log = LoggerFactory.getLogger(KakaoMapRoutingClient.class);
    private static final int NEARBY_OFFSET_METERS = 250;
    private static final int MAX_NEARBY_RETRIES = 4;
    private static final long WALK_SECONDS_PER_METER = 1;

    private final RestClient restClient;
    private final KakaoClientExecutor executor;

    public KakaoMapRoutingClient(
            @Qualifier("kakaoLocalRestClient") RestClient restClient, KakaoClientExecutor executor) {
        this.restClient = restClient;
        this.executor = executor;
    }

    public KakaoTravelRoute findWalkingRoute(KakaoRouteRequest request) {
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
                    || response.route().properties().totalDistance() == null
                    || response.route().properties().totalDistance() < 0
                    || response.route().properties().totalTime() == null
                    || response.route().properties().totalTime() < 0) {
                throw new BusinessException(ErrorCode.ROUTE_NOT_FOUND);
            }
            return new KakaoTravelRoute(
                    response.route().properties().totalDistance(),
                    Duration.ofSeconds(response.route().properties().totalTime()));
        });
    }

    public KakaoTravelRoute findPublicTransitRoute(KakaoRouteRequest request) {
        return executor.execute("publicTransitRoute", () -> {
            KakaoPublicTransitResponse response = requestPublicTransitRoute(request);
            KakaoTravelRoute route = fastestRoute(response);
            if (route != null) {
                return route;
            }

            if (!needsNearbyRetry(response)) {
                log.warn("Kakao public transit route not found: providerStatus={}", providerStatus(response));
                throw new BusinessException(ErrorCode.ROUTE_NOT_FOUND);
            }

            KakaoCoordinate nearbyOrigin = request.origin();
            KakaoCoordinate nearbyDestination = request.destination();
            int originDirectionIndex = 0;
            int destinationDirectionIndex = 0;
            int attempts = 0;
            while (attempts < MAX_NEARBY_RETRIES && needsNearbyRetry(response)) {
                boolean adjustOrigin = "STARTNODES_NULL".equals(response.status())
                        || ("NO_RESULTS".equals(response.status()) && attempts % 2 == 0);
                List<OffsetDirection> directions = adjustOrigin
                        ? offsetDirections(request.origin(), request.destination())
                        : offsetDirections(request.destination(), request.origin());
                int directionIndex = adjustOrigin ? originDirectionIndex++ : destinationDirectionIndex++;
                if (directionIndex >= directions.size()) {
                    break;
                }

                if (adjustOrigin) {
                    nearbyOrigin = offset(request.origin(), directions.get(directionIndex));
                } else {
                    nearbyDestination = offset(request.destination(), directions.get(directionIndex));
                }
                attempts++;
                response = requestPublicTransitRoute(new KakaoRouteRequest(nearbyOrigin, nearbyDestination));
                KakaoTravelRoute nearbyRoute = fastestRoute(response);
                if (nearbyRoute != null) {
                    int addedWalkMeters = (nearbyOrigin.equals(request.origin()) ? 0 : NEARBY_OFFSET_METERS)
                            + (nearbyDestination.equals(request.destination()) ? 0 : NEARBY_OFFSET_METERS);
                    String adjustedEndpoints;
                    if (!nearbyOrigin.equals(request.origin()) && !nearbyDestination.equals(request.destination())) {
                        adjustedEndpoints = "both";
                    } else {
                        adjustedEndpoints = nearbyOrigin.equals(request.origin()) ? "destination" : "origin";
                    }
                    log.info(
                            "Kakao public transit route found after nearby retry: endpoint={}, offsetMeters={}",
                            adjustedEndpoints,
                            addedWalkMeters);
                    return new KakaoTravelRoute(
                            nearbyRoute.distanceMeters() + addedWalkMeters,
                            nearbyRoute.duration().plusSeconds(addedWalkMeters * WALK_SECONDS_PER_METER));
                }
            }

            log.warn(
                    "Kakao public transit route not found after nearby retries: providerStatus={}, attempts={}",
                    providerStatus(response),
                    attempts);
            throw new BusinessException(ErrorCode.ROUTE_NOT_FOUND);
        });
    }

    private boolean needsNearbyRetry(KakaoPublicTransitResponse response) {
        return response != null
                && ("STARTNODES_NULL".equals(response.status())
                        || "ENDNODES_NULL".equals(response.status())
                        || "NO_RESULTS".equals(response.status()));
    }

    private KakaoPublicTransitResponse requestPublicTransitRoute(KakaoRouteRequest request) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/v2/routing/publictraffic")
                        .queryParam("start_x", request.origin().longitude().toPlainString())
                        .queryParam("start_y", request.origin().latitude().toPlainString())
                        .queryParam("end_x", request.destination().longitude().toPlainString())
                        .queryParam("end_y", request.destination().latitude().toPlainString())
                        .build())
                .retrieve()
                .body(KakaoPublicTransitResponse.class);
    }

    private KakaoTravelRoute fastestRoute(KakaoPublicTransitResponse response) {
        if (response == null || !"OK".equals(response.status()) || response.routes() == null) {
            return null;
        }
        return response.routes().stream()
                .filter(Objects::nonNull)
                .map(KakaoPublicTransitResponse.Route::properties)
                .filter(Objects::nonNull)
                .filter(route -> isBusOrSubway(route.type()))
                .filter(route -> route.totalTime() != null && route.totalTime() >= 0)
                .filter(route -> route.totalDistance() != null && route.totalDistance() >= 0)
                .min(Comparator.comparing(KakaoPublicTransitResponse.RouteProperties::totalTime))
                .map(route -> new KakaoTravelRoute(route.totalDistance(), Duration.ofSeconds(route.totalTime())))
                .orElse(null);
    }

    private boolean isBusOrSubway(String routeType) {
        return "BUS".equals(routeType) || "SUBWAY".equals(routeType);
    }

    private List<OffsetDirection> offsetDirections(KakaoCoordinate point, KakaoCoordinate otherPoint) {
        double latitude = point.latitude().doubleValue();
        double latitudeScale = 111_320.0;
        double longitudeScale = latitudeScale * Math.cos(Math.toRadians(latitude));
        if (Math.abs(longitudeScale) < 1.0) {
            return List.of();
        }

        double east = (otherPoint.longitude().doubleValue() - point.longitude().doubleValue())
                * longitudeScale;
        double north = (otherPoint.latitude().doubleValue() - latitude) * latitudeScale;
        double length = Math.hypot(east, north);
        if (length < 1.0) {
            east = 0;
            north = 1;
        } else {
            east /= length;
            north /= length;
        }

        return List.of(
                new OffsetDirection(east, north),
                new OffsetDirection(-north, east),
                new OffsetDirection(north, -east),
                new OffsetDirection(-east, -north));
    }

    private KakaoCoordinate offset(KakaoCoordinate point, OffsetDirection direction) {
        double latitude = point.latitude().doubleValue()
                + direction.north() * NEARBY_OFFSET_METERS / 111_320.0;
        double longitudeScale = 111_320.0 * Math.cos(Math.toRadians(point.latitude().doubleValue()));
        double longitude = point.longitude().doubleValue()
                + direction.east() * NEARBY_OFFSET_METERS / longitudeScale;
        return new KakaoCoordinate(
                BigDecimal.valueOf(longitude).setScale(7, RoundingMode.HALF_UP),
                BigDecimal.valueOf(latitude).setScale(7, RoundingMode.HALF_UP));
    }

    private String providerStatus(KakaoPublicTransitResponse response) {
        return response == null ? "NO_RESPONSE" : response.status();
    }

    private record OffsetDirection(double east, double north) {}
}
