package org.zerozero.homerunhome.kakao.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.util.UriUtils;
import org.springframework.web.client.RestClient;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.kakao.client.local.KakaoLocalClient;
import org.zerozero.homerunhome.kakao.client.mobility.KakaoMobilityClient;
import org.zerozero.homerunhome.kakao.client.support.KakaoClientExecutor;
import org.zerozero.homerunhome.kakao.dto.common.KakaoCoordinate;
import org.zerozero.homerunhome.kakao.dto.request.KakaoAddressSearchRequest;
import org.zerozero.homerunhome.kakao.dto.request.KakaoPlaceSearchRequest;
import org.zerozero.homerunhome.kakao.dto.request.KakaoPlaceSearchSort;
import org.zerozero.homerunhome.kakao.dto.request.KakaoRouteRequest;
import org.zerozero.homerunhome.kakao.dto.response.KakaoRoute;

class KakaoApiClientTest {

    private static final String API_KEY = "test-rest-api-key";

    private MockRestServiceServer localServer;
    private MockRestServiceServer mobilityServer;
    private KakaoApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder localBuilder = RestClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + API_KEY);
        RestClient.Builder mobilityBuilder = RestClient.builder()
                .baseUrl("https://apis-navi.kakaomobility.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + API_KEY);
        localServer = MockRestServiceServer.bindTo(localBuilder).build();
        mobilityServer = MockRestServiceServer.bindTo(mobilityBuilder).build();
        KakaoClientExecutor executor = new KakaoClientExecutor(API_KEY);
        client = new KakaoApiClient(
                new KakaoLocalClient(localBuilder.build(), executor),
                new KakaoMobilityClient(mobilityBuilder.build(), executor));
    }

    @Test
    void searchesAddressAndMapsCoordinate() {
        localServer.expect(requestTo(Matchers.containsString("/v2/local/search/address.json")))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK " + API_KEY))
                .andExpect(queryParam(
                        "query", UriUtils.encode("대전 한화생명 볼파크", StandardCharsets.UTF_8)))
                .andExpect(queryParam("page", "1"))
                .andExpect(queryParam("size", "10"))
                .andRespond(withSuccess(
                        """
                        {
                          "meta":{"total_count":1,"pageable_count":1,"is_end":true},
                          "documents":[{
                            "address_name":"대전 중구 부사동 209-1",
                            "x":"127.429188",
                            "y":"36.317067",
                            "road_address":{"address_name":"대전 중구 대종로 373"}
                          }]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        var result = client.searchAddresses(KakaoAddressSearchRequest.firstPage("대전 한화생명 볼파크"));

        assertThat(result.items()).hasSize(1);
        assertEquals("대전 중구 대종로 373", result.items().getFirst().roadAddressName());
        assertEquals(new BigDecimal("127.429188"), result.items().getFirst().coordinate().longitude());
        assertEquals(1, result.totalCount());
        assertThat(result.end()).isTrue();
        localServer.verify();
    }

    @Test
    void searchesPlacesAroundCoordinateAndMapsDistance() {
        localServer.expect(requestTo(Matchers.containsString("/v2/local/search/keyword.json")))
                .andExpect(queryParam("query", UriUtils.encode("성심당", StandardCharsets.UTF_8)))
                .andExpect(queryParam("x", "127.429188"))
                .andExpect(queryParam("y", "36.317067"))
                .andExpect(queryParam("radius", "3000"))
                .andExpect(queryParam("sort", "distance"))
                .andRespond(withSuccess(
                        """
                        {
                          "meta":{"total_count":1,"pageable_count":1,"is_end":true},
                          "documents":[{
                            "id":"place-1",
                            "place_name":"성심당 본점",
                            "category_name":"음식점 > 제과,베이커리",
                            "category_group_code":"FD6",
                            "phone":"1588-8069",
                            "address_name":"대전 중구 은행동 145",
                            "road_address_name":"대전 중구 대종로480번길 15",
                            "x":"127.427314",
                            "y":"36.327681",
                            "place_url":"http://place.map.kakao.com/place-1",
                            "distance":"1194"
                          }]
                        }
                        """,
                        MediaType.APPLICATION_JSON));
        KakaoCoordinate center = new KakaoCoordinate(new BigDecimal("127.429188"), new BigDecimal("36.317067"));
        KakaoPlaceSearchRequest request =
                new KakaoPlaceSearchRequest("성심당", center, 3000, KakaoPlaceSearchSort.DISTANCE, 1, 15);

        var result = client.searchPlaces(request);

        assertEquals("성심당 본점", result.items().getFirst().name());
        assertEquals(1194, result.items().getFirst().distanceMeters());
        assertEquals("FD6", result.items().getFirst().categoryGroupCode());
        localServer.verify();
    }

    @Test
    void returnsCarModeDistanceAndDurationFromDrivingRoute() {
        mobilityServer.expect(requestTo(Matchers.containsString("/v1/directions")))
                .andExpect(queryParam("origin", "127.429188,36.317067"))
                .andExpect(queryParam("destination", "127.427314,36.327681"))
                .andExpect(queryParam("summary", "true"))
                .andRespond(withSuccess(
                        """
                        {
                          "routes":[{
                            "result_code":0,
                            "summary":{"distance":1800,"duration":420,"fare":{"taxi":5500,"toll":0}}
                          }]
                        }
                        """,
                        MediaType.APPLICATION_JSON));
        KakaoRouteRequest request = new KakaoRouteRequest(
                new KakaoCoordinate(new BigDecimal("127.429188"), new BigDecimal("36.317067")),
                new KakaoCoordinate(new BigDecimal("127.427314"), new BigDecimal("36.327681")));

        KakaoRoute route = client.findDrivingRoute(request);

        assertEquals(KakaoRoute.TravelMode.CAR, route.travelMode());
        assertEquals(1800, route.distanceMeters());
        assertEquals(Duration.ofMinutes(7), route.duration());
        assertEquals(5500, route.taxiFareWon());
        mobilityServer.verify();
    }

    @Test
    void distinguishesAuthenticationFailure() {
        localServer.expect(requestTo(Matchers.any(String.class)))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> client.searchAddresses(KakaoAddressSearchRequest.firstPage("대전역")));

        assertEquals(ErrorCode.KAKAO_AUTHENTICATION_FAILED, exception.getErrorCode());
    }

    @Test
    void distinguishesQuotaExceeded() {
        localServer.expect(requestTo(Matchers.any(String.class)))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> client.searchPlaces(KakaoPlaceSearchRequest.firstPage("카페")));

        assertEquals(ErrorCode.KAKAO_QUOTA_EXCEEDED, exception.getErrorCode());
    }

    @Test
    void distinguishesExternalServerFailure() {
        mobilityServer.expect(requestTo(Matchers.any(String.class))).andRespond(withServerError());
        KakaoCoordinate coordinate = new KakaoCoordinate(BigDecimal.ZERO, BigDecimal.ZERO);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> client.findDrivingRoute(new KakaoRouteRequest(coordinate, coordinate)));

        assertEquals(ErrorCode.KAKAO_EXTERNAL_SERVER_ERROR, exception.getErrorCode());
    }

    @Test
    void failsBeforeCallingProviderWhenApiKeyIsMissing() {
        KakaoClientExecutor executor = new KakaoClientExecutor(" ");
        KakaoApiClient clientWithoutKey = new KakaoApiClient(
                new KakaoLocalClient(RestClient.create(), executor),
                new KakaoMobilityClient(RestClient.create(), executor));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> clientWithoutKey.searchAddresses(KakaoAddressSearchRequest.firstPage("대전역")));

        assertEquals(ErrorCode.KAKAO_API_KEY_NOT_CONFIGURED, exception.getErrorCode());
    }
}
