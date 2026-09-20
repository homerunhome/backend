package org.zerozero.homerunhome.train;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.train.client.TagoTrainClient;

class TagoTrainClientTest {

    private static final String BASE_URL = "https://apis.data.go.kr/1613000/TrainInfoService";

    private MockRestServiceServer server;
    private TagoTrainClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TagoTrainClient(builder, BASE_URL, "test-service-key");
    }

    @Test
    void requestsKtxTimetableAndMapsScheduleFields() {
        server.expect(requestTo(containsString("/getStrtpntAlocFndTrainInfo")))
                .andExpect(queryParam("serviceKey", "test-service-key"))
                .andExpect(queryParam("depPlaceId", "NAT010000"))
                .andExpect(queryParam("arrPlaceId", "NAT011668"))
                .andExpect(queryParam("depPlandTime", "20260920"))
                .andExpect(queryParam("trainGradeCode", "00"))
                .andRespond(withSuccess(
                        """
                        {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":[
                          {"trainno":"001","traingradename":"KTX","depplacename":"서울","depplandtime":"202609200800","arrplacename":"대전","arrplandtime":"202609200900"},
                          {"trainno":"101","traingradename":"무궁화호","depplacename":"서울","depplandtime":"202609200810","arrplacename":"대전","arrplandtime":"202609201000"}
                        ]}}}}
                        """,
                        MediaType.APPLICATION_JSON));

        var schedules = client.findKtxSchedules("NAT010000", "NAT011668", LocalDate.of(2026, 9, 20));

        assertEquals(1, schedules.size());
        assertEquals("001", schedules.getFirst().trainNumber());
        assertEquals("서울", schedules.getFirst().departureStation());
        assertEquals("2026-09-20T08:00", schedules.getFirst().departureAt().toString());
        assertEquals("2026-09-20T09:00", schedules.getFirst().arrivalAt().toString());
        server.verify();
    }

    @Test
    void failsClearlyWhenServiceKeyIsMissing() {
        TagoTrainClient clientWithoutKey = new TagoTrainClient(RestClient.builder(), BASE_URL, " ");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> clientWithoutKey.findKtxSchedules("NAT010000", "NAT011668", LocalDate.now()));

        assertEquals(ErrorCode.TAGO_API_KEY_NOT_CONFIGURED, exception.getErrorCode());
    }

    @Test
    void looksUpTrainCitiesAndStations() {
        server.expect(requestTo(containsString("/getCtyCodeList")))
                .andRespond(withSuccess(
                        """
                        {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":{"citycode":"11","cityname":"서울특별시"}}}}}
                        """,
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/getCtyAcctoTrainSttnList")))
                .andExpect(queryParam("cityCode", "11"))
                .andRespond(withSuccess(
                        """
                        {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":{"nodeid":"NAT010000","nodename":"서울","citycode":"11"}}}}}
                        """,
                        MediaType.APPLICATION_JSON));

        var cities = client.findCities();
        var stations = client.findStations("11");

        assertEquals("서울특별시", cities.getFirst().cityName());
        assertEquals("NAT010000", stations.getFirst().stationId());
        assertEquals("서울", stations.getFirst().stationName());
        server.verify();
    }
}
