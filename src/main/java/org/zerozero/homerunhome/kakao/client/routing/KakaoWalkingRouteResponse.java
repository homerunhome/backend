package org.zerozero.homerunhome.kakao.client.routing;

public record KakaoWalkingRouteResponse(String status, Route route) {

    public record Route(Properties properties) {}

    public record Properties(Integer totalDistance, Integer totalTime) {}
}
