package org.zerozero.homerunhome.train;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.train.client.TagoTrainClient;

class TagoTrainClientTest {

    private static final String BASE_URL = "https://apis.data.go.kr/1613000/TrainInfo";

    private MockRestServiceServer server;
    private TagoTrainClient client;
    private ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TagoTrainClient(builder, BASE_URL, "test-service-key");
        Logger logger = (Logger) LoggerFactory.getLogger(TagoTrainClient.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(TagoTrainClient.class)).detachAppender(logAppender);
    }

    @Test
    void requestsKtxTimetableAndMapsScheduleFields() {
        server.expect(requestTo(containsString("/GetStrtpntAlocFndTrainInfo")))
                .andExpect(queryParam("serviceKey", "test-service-key"))
                .andExpect(queryParam("depPlaceId", "NAT010000"))
                .andExpect(queryParam("arrPlaceId", "NAT011668"))
                .andExpect(queryParam("depPlandTime", "20260920"))
                .andExpect(queryParam("trainGradeCode", "00"))
                .andRespond(withSuccess(
                        """
                        {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":[
                          {"trainno":"001","traingradename":"KTX","depplacename":"서울","depplandtime":"20260920080000","arrplacename":"대전","arrplandtime":"20260920090000"},
                          {"trainno":"101","traingradename":"무궁화호","depplacename":"서울","depplandtime":"20260920081000","arrplacename":"대전","arrplandtime":"20260920100000"}
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
        server.expect(requestTo(containsString("/GetCtyCodeList")))
                .andRespond(withSuccess(
                        """
                        {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":{"citycode":"11","cityname":"서울특별시"}}}}}
                        """,
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/GetCtyAcctoTrainSttnList")))
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

    @Test
    void looksUpTrainGradesAndMapsNonKtxSchedules() {
        server.expect(requestTo(containsString("/GetVhcleKndList")))
                .andRespond(withSuccess(
                        """
                        {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":[
                          {"vehiclekndid":"00","vehiclekndnm":"KTX"},
                          {"vehiclekndid":"02","vehiclekndnm":"무궁화호"}
                        ]}}}}
                        """,
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/GetStrtpntAlocFndTrainInfo")))
                .andExpect(queryParam("depPlaceId", "NAT010000"))
                .andExpect(queryParam("arrPlaceId", "NAT011668"))
                .andExpect(queryParam("trainGradeCode", "02"))
                .andRespond(withSuccess(
                        """
                        {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":
                          {"trainno":"101","traingradename":"무궁화호","depplacename":"서울","depplandtime":"20260920081035","arrplacename":"대전","arrplandtime":"20260920100042"}
                        }}}}
                        """,
                        MediaType.APPLICATION_JSON));

        var grades = client.findTrainGradeCodes();
        var schedules = client.findSchedulesByGradeCode(
                "NAT010000", "NAT011668", LocalDate.of(2026, 9, 20), "02");

        assertEquals(java.util.List.of("00", "02"), grades);
        assertEquals("무궁화호", schedules.getFirst().trainType());
        assertEquals(LocalDateTime.of(2026, 9, 20, 8, 10, 35), schedules.getFirst().departureAt());
        assertEquals(LocalDateTime.of(2026, 9, 20, 10, 0, 42), schedules.getFirst().arrivalAt());
        server.verify();
    }

    @Test
    void logsProviderErrorCodeWithoutLoggingResponseMessageOrServiceKey() {
        server.expect(requestTo(containsString("/GetCtyCodeList")))
                .andRespond(withSuccess(
                        """
                        {"response":{"header":{"resultCode":"30","resultMsg":"rejected test-service-key"}}}
                        """,
                        MediaType.APPLICATION_JSON));

        assertThrows(BusinessException.class, client::findCities);

        String logs = logAppender.list.stream()
                .map(event -> event.getFormattedMessage())
                .collect(Collectors.joining("\n"));
        assertTrue(logs.contains("operation=GetCtyCodeList"));
        assertTrue(logs.contains("resultCode=30"));
        assertFalse(logs.contains("test-service-key"));
        assertFalse(logs.contains("rejected"));
        server.verify();
    }

    @Test
    void logsHttpStatusWithoutLoggingServiceKey() {
        server.expect(requestTo(containsString("/GetCtyCodeList")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(BusinessException.class, client::findCities);

        String logs = logAppender.list.stream()
                .map(event -> event.getFormattedMessage())
                .collect(Collectors.joining("\n"));
        assertTrue(logs.contains("operation=GetCtyCodeList"));
        assertTrue(logs.contains("httpStatus=429"));
        assertFalse(logs.contains("test-service-key"));
        server.verify();
    }

    @Test
    void logsInvalidScheduleDateTimeLengthWithoutLoggingItsValue() {
        server.expect(requestTo(containsString("/GetStrtpntAlocFndTrainInfo")))
                .andRespond(withSuccess(
                        """
                        {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":
                          {"trainno":"101","traingradename":"무궁화호","depplacename":"서울","depplandtime":"202609200810123","arrplacename":"대전","arrplandtime":"20260920100042"}
                        }}}}
                        """,
                        MediaType.APPLICATION_JSON));

        assertThrows(
                BusinessException.class,
                () -> client.findSchedulesByGradeCode(
                        "NAT010000", "NAT011668", LocalDate.of(2026, 9, 20), "02"));

        String logs = logAppender.list.stream()
                .map(event -> event.getFormattedMessage())
                .collect(Collectors.joining("\n"));
        assertTrue(logs.contains("valueLength=15"));
        assertFalse(logs.contains("202609200810123"));
        server.verify();
    }
}
