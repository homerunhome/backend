package org.zerozero.homerunhome.tourapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.tourapi.client.BarrierFreeTourApiClient;
import org.zerozero.homerunhome.tourapi.client.KoreanTourApiClient;
import org.zerozero.homerunhome.tourapi.client.TourApiClientExecutor;
import org.zerozero.homerunhome.tourapi.client.TourApiResponseParser;
import tools.jackson.databind.ObjectMapper;

class TourApiClientTest {

    private static final String SERVICE_KEY = "test-service-key";

    private MockRestServiceServer koreanServer;
    private MockRestServiceServer barrierFreeServer;
    private KoreanTourApiClient koreanClient;
    private BarrierFreeTourApiClient barrierFreeClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder koreanBuilder = RestClient.builder()
                .baseUrl("https://apis.data.go.kr/B551011/KorService2");
        RestClient.Builder barrierFreeBuilder = RestClient.builder()
                .baseUrl("https://apis.data.go.kr/B551011/KorWithService2");
        koreanServer = MockRestServiceServer.bindTo(koreanBuilder).build();
        barrierFreeServer = MockRestServiceServer.bindTo(barrierFreeBuilder).build();
        TourApiClientExecutor executor = new TourApiClientExecutor(
                SERVICE_KEY, "HomerunHome", new TourApiResponseParser(new ObjectMapper()));
        koreanClient = new KoreanTourApiClient(koreanBuilder.build(), executor);
        barrierFreeClient = new BarrierFreeTourApiClient(barrierFreeBuilder.build(), executor);
    }

    @Test
    void findsDaejeonTourismContents() {
        koreanServer.expect(requestTo(Matchers.containsString("/KorService2/areaBasedList2")))
                .andExpect(queryParam("serviceKey", SERVICE_KEY))
                .andExpect(queryParam("MobileOS", "ETC"))
                .andExpect(queryParam("MobileApp", "HomerunHome"))
                .andExpect(queryParam("areaCode", "3"))
                .andExpect(queryParam("contentTypeId", "12"))
                .andExpect(queryParam("pageNo", "1"))
                .andExpect(queryParam("numOfRows", "12"))
                .andRespond(withSuccess(successResponse("""
                        [{
                          "contentid":"125407",
                          "contenttypeid":"12",
                          "title":"한밭수목원",
                          "addr1":"대전 서구 둔산대로 169",
                          "firstimage":"https://example.com/image.jpg",
                          "mapx":"127.388",
                          "mapy":"36.366"
                        }]
                        """, 1), MediaType.APPLICATION_JSON));

        var response = koreanClient.findDaejeonContents("12", 1, 12);

        assertThat(response.items()).hasSize(1);
        assertEquals("한밭수목원", response.items().getFirst().title());
        assertEquals("127.388", response.items().getFirst().longitude().toPlainString());
        assertEquals(1, response.totalCount());
        koreanServer.verify();
    }

    @Test
    void findsDetailImagesAndBarrierFreeInformation() {
        koreanServer.expect(requestTo(Matchers.containsString("/KorService2/detailCommon2")))
                .andExpect(queryParam("contentId", "125407"))
                .andExpect(queryParam("overviewYN", "Y"))
                .andRespond(withSuccess(successResponse("""
                        [{
                          "contentid":"125407",
                          "contenttypeid":"12",
                          "title":"한밭수목원",
                          "overview":"도심 속 수목원"
                        }]
                        """, 1), MediaType.APPLICATION_JSON));
        koreanServer.expect(requestTo(Matchers.containsString("/KorService2/detailImage2")))
                .andExpect(queryParam("contentId", "125407"))
                .andExpect(queryParam("imageYN", "Y"))
                .andRespond(withSuccess(successResponse("""
                        [{
                          "imgname":"한밭수목원 전경",
                          "originimgurl":"https://example.com/original.jpg",
                          "smallimageurl":"https://example.com/small.jpg",
                          "cpyrhtDivCd":"Type1"
                        }]
                        """, 1), MediaType.APPLICATION_JSON));
        barrierFreeServer.expect(requestTo(Matchers.containsString("/KorWithService2/detailWithTour2")))
                .andExpect(queryParam("contentId", "125407"))
                .andRespond(withSuccess(successResponse("""
                        [{"contentid":"125407","parking":"장애인 주차구역 있음","wheelchair":"대여 가능"}]
                        """, 1), MediaType.APPLICATION_JSON));

        var detail = koreanClient.findDetail("125407");
        var images = koreanClient.findImages("125407");
        var accessibility = barrierFreeClient.findDetail("125407");

        assertThat(detail).isPresent().get().extracting(content -> content.title()).isEqualTo("한밭수목원");
        assertThat(images).singleElement().extracting(image -> image.name()).isEqualTo("한밭수목원 전경");
        assertThat(accessibility).isPresent().get()
                .extracting(info -> info.wheelchair())
                .isEqualTo("대여 가능");
        koreanServer.verify();
        barrierFreeServer.verify();
    }

    @Test
    void findsDaejeonEventsForDateRange() {
        koreanServer.expect(requestTo(Matchers.containsString("/KorService2/searchFestival2")))
                .andExpect(queryParam("eventStartDate", "20260920"))
                .andExpect(queryParam("eventEndDate", "20260921"))
                .andExpect(queryParam("areaCode", "3"))
                .andRespond(withSuccess(successResponse("""
                        [{
                          "contentid":"event-1",
                          "contenttypeid":"15",
                          "title":"대전 축제",
                          "eventstartdate":"20260920",
                          "eventenddate":"20260921"
                        }]
                        """, 1), MediaType.APPLICATION_JSON));

        var response = koreanClient.findDaejeonEvents(
                LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 21), 1, 12);

        assertThat(response.items()).singleElement()
                .extracting(event -> event.eventStartDate())
                .isEqualTo("20260920");
        koreanServer.verify();
    }

    @ParameterizedTest
    @CsvSource({
        "30, SERVICE_KEY_IS_NOT_REGISTERED_ERROR, TOUR_API_AUTHENTICATION_FAILED",
        "22, LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR, TOUR_API_QUOTA_EXCEEDED",
        "10, INVALID_REQUEST_PARAMETER_ERROR, TOUR_API_INVALID_REQUEST"
    })
    void distinguishesProviderFailures(String code, String message, ErrorCode expectedError) {
        expectProviderFailure(code, message);

        assertError(expectedError);
    }

    @Test
    void distinguishesExternalServerFailure() {
        koreanServer.expect(requestTo(Matchers.any(String.class))).andRespond(withServerError());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> koreanClient.findDaejeonContents("12", 1, 12));

        assertEquals(ErrorCode.TOUR_API_EXTERNAL_SERVER_ERROR, exception.getErrorCode());
    }

    @Test
    void distinguishesXmlAuthenticationFailureReturnedByDataPortalGateway() {
        koreanServer.expect(requestTo(Matchers.any(String.class)))
                .andRespond(withSuccess("""
                        <OpenAPI_ServiceResponse>
                          <cmmMsgHeader>
                            <returnReasonCode>30</returnReasonCode>
                            <returnAuthMsg>SERVICE_KEY_IS_NOT_REGISTERED_ERROR</returnAuthMsg>
                            <resultCode>30</resultCode>
                            <resultMsg>SERVICE_KEY_IS_NOT_REGISTERED_ERROR</resultMsg>
                          </cmmMsgHeader>
                        </OpenAPI_ServiceResponse>
                        """, MediaType.APPLICATION_XML));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> koreanClient.findDaejeonContents("12", 1, 12));

        assertEquals(ErrorCode.TOUR_API_AUTHENTICATION_FAILED, exception.getErrorCode());
    }

    @Test
    void failsBeforeCallingProviderWhenKeyIsMissing() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.com");
        KoreanTourApiClient clientWithoutKey = new KoreanTourApiClient(
                builder.build(),
                new TourApiClientExecutor(
                        " ", "HomerunHome", new TourApiResponseParser(new ObjectMapper())));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> clientWithoutKey.findDaejeonContents("12", 1, 12));

        assertEquals(ErrorCode.TOUR_API_KEY_NOT_CONFIGURED, exception.getErrorCode());
    }

    private void expectProviderFailure(String code, String message) {
        koreanServer.expect(requestTo(Matchers.any(String.class)))
                .andRespond(withSuccess("""
                        {
                          "response":{
                            "header":{"resultCode":"%s","resultMsg":"%s"},
                            "body":{"items":"","numOfRows":0,"pageNo":1,"totalCount":0}
                          }
                        }
                        """.formatted(code, message), MediaType.APPLICATION_JSON));
    }

    private void assertError(ErrorCode errorCode) {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> koreanClient.findDaejeonContents("12", 1, 12));
        assertEquals(errorCode, exception.getErrorCode());
    }

    private String successResponse(String items, int totalCount) {
        return """
                {
                  "response":{
                    "header":{"resultCode":"0000","resultMsg":"OK"},
                    "body":{"items":{"item":%s},"numOfRows":12,"pageNo":1,"totalCount":%d}
                  }
                }
                """.formatted(items, totalCount);
    }
}
