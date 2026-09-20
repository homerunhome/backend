package org.zerozero.homerunhome.kakao.client.mobility;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record KakaoRouteResponse(List<Route> routes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Route(@JsonProperty("result_code") int resultCode, Summary summary) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Summary(int distance, int duration, Fare fare) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Fare(int taxi, int toll) {}
}
