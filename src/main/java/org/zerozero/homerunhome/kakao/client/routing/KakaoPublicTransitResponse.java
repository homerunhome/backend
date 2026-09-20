package org.zerozero.homerunhome.kakao.client.routing;

import java.util.List;

public record KakaoPublicTransitResponse(String status, List<Route> routes) {

    public record Route(RouteProperties properties) {}

    public record RouteProperties(String type, Integer totalDistance, Integer totalTime, Integer transfers) {}
}
