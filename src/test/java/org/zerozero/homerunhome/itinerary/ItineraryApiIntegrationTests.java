package org.zerozero.homerunhome.itinerary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.zerozero.homerunhome.game.domain.Game;
import org.zerozero.homerunhome.game.repository.GameRepository;
import org.zerozero.homerunhome.itinerary.repository.ItineraryRepository;

@SpringBootTest
@ActiveProfiles("test")
class ItineraryApiIntegrationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ItineraryRepository itineraryRepository;

    @Autowired
    private GameRepository gameRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        itineraryRepository.deleteAll();
        gameRepository.deleteAll();
        gameRepository.save(Game.builder()
                .gameId("game-2026-09-20-001")
                .city("대전")
                .gameDate(LocalDate.of(2026, 9, 20))
                .gameStartTime(LocalTime.of(18, 30))
                .stadium("대전 한화생명 볼파크")
                .homeTeam("한화 이글스")
                .awayTeam("롯데 자이언츠")
                .build());
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void createsAndReadsItineraryWithOrderedPlaces() throws Exception {
        String request = """
                {
                  "gameId": "game-2026-09-20-001",
                  "arrivalPlace": "서울역",
                  "arrivalAt": "2026-09-20T12:00:00",
                  "departurePlace": "서울역",
                  "departureAt": "2026-09-20T23:30:00",
                  "preferences": ["FOOD", "CAFE"],
                  "places": [
                    {"placeId": "place-1", "name": "첫 번째 장소", "address": "서울시", "latitude": 37.5, "longitude": 127.0},
                    {"placeId": "place-2", "name": "두 번째 장소", "address": "서울시", "latitude": 37.6, "longitude": 127.1}
                  ]
                }
                """;

        String location = mockMvc.perform(post("/api/itineraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        String id = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(get("/api/itineraries/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value("game-2026-09-20-001"))
                .andExpect(jsonPath("$.gameDate").value("2026-09-20"))
                .andExpect(jsonPath("$.stadium").value("대전 한화생명 볼파크"))
                .andExpect(jsonPath("$.homeTeam").value("한화 이글스"))
                .andExpect(jsonPath("$.arrivalPlace").value("서울역"))
                .andExpect(jsonPath("$.preferences[0]").value("FOOD"))
                .andExpect(jsonPath("$.places.length()").value(2))
                .andExpect(jsonPath("$.places[0].name").value("첫 번째 장소"))
                .andExpect(jsonPath("$.places[1].name").value("두 번째 장소"));
    }

    @Test
    void generatesAndPersistsCalculatedCourseAndGameEndEstimate() throws Exception {
        String location = mockMvc.perform(post("/api/itineraries/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameId":"game-2026-09-20-001",
                                  "arrivalPlace":"대전역",
                                  "arrivalAt":"2026-09-20T12:00:00",
                                  "departurePlace":"대전역",
                                  "departureAt":"2026-09-21T00:30:00",
                                  "stadiumEntryBufferMinutes":30,
                                  "finalLegToStadiumMinutes":20,
                                  "stadiumToDepartureMinutes":45,
                                  "postGameCrowdBufferMinutes":30,
                                  "boardingBufferMinutes":15,
                                  "places":[
                                    {"placeId":"tour-1","name":"한밭수목원","travelFromPreviousMinutes":20,"stayDurationMinutes":60,"mustVisit":true},
                                    {"placeId":"food-1","name":"식당","travelFromPreviousMinutes":15,"stayDurationMinutes":45}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.stadiumArrivalAt").value("2026-09-20T14:40:00"))
                .andExpect(jsonPath("$.gameStartSlackMinutes").value(230))
                .andExpect(jsonPath("$.stadiumDepartureRecommendedAt").value("2026-09-20T23:00:00"))
                .andExpect(jsonPath("$.expectedGameEndAt").value("2026-09-20T21:30:00"))
                .andExpect(jsonPath("$.gameEndMayBeDelayed").value(true))
                .andExpect(jsonPath("$.status").value("AMPLE"))
                .andExpect(jsonPath("$.places[0].arrivesAt").value("2026-09-20T12:20:00"))
                .andExpect(jsonPath("$.places[1].arrivesAt").value("2026-09-20T13:35:00"))
                .andExpect(jsonPath("$.timeline.length()").value(8))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        String id = location.substring(location.lastIndexOf('/') + 1);
        mockMvc.perform(get("/api/itineraries/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expectedGameEndAt").value("2026-09-20T21:30:00"))
                .andExpect(jsonPath("$.gameEndMayBeDelayed").value(true));
    }

    @Test
    void returnsOvertimeAndTravelWarningsWhenPlanDoesNotFit() throws Exception {
        mockMvc.perform(post("/api/itineraries/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameId":"game-2026-09-20-001",
                                  "arrivalPlace":"대전역",
                                  "arrivalAt":"2026-09-20T17:30:00",
                                  "departurePlace":"대전역",
                                  "departureAt":"2026-09-20T22:00:00",
                                  "stadiumEntryBufferMinutes":30,
                                  "finalLegToStadiumMinutes":10,
                                  "stadiumToDepartureMinutes":60,
                                  "postGameCrowdBufferMinutes":30,
                                  "boardingBufferMinutes":15,
                                  "places":[
                                    {"placeId":"tour-1","name":"한밭수목원","travelFromPreviousMinutes":20,"stayDurationMinutes":50}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stadiumArrivalAt").value("2026-09-20T18:50:00"))
                .andExpect(jsonPath("$.status").value("RETURN_CAUTION"))
                .andExpect(jsonPath("$.warnings.length()").value(3))
                .andExpect(jsonPath("$.places.length()").value(1))
                .andExpect(jsonPath("$.places[0].scheduled").value(true));
    }

    @Test
    void rejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/itineraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.gameId").exists());
    }

    @Test
    void updatesSavedItinerary() throws Exception {
        String location = mockMvc.perform(post("/api/itineraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameId":"game-2026-09-20-001",
                                  "arrivalPlace":"대전역",
                                  "arrivalAt":"2026-09-20T12:00:00",
                                  "departurePlace":"서울역",
                                  "departureAt":"2026-09-20T23:30:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        String id = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(put("/api/itineraries/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameId":"game-2026-09-20-001",
                                  "arrivalPlace":"대전역",
                                  "arrivalAt":"2026-09-20T12:00:00",
                                  "departurePlace":"서울역",
                                  "departureAt":"2026-09-20T23:30:00",
                                  "preferences":["FOOD","CAFE"],
                                  "places":[
                                    {"placeId":"place-2","name":"수정한 장소","address":"대전시","latitude":36.3,"longitude":127.4,"arrivesAt":"2026-09-20T13:00:00","departsAt":"2026-09-20T14:00:00","stayDurationMinutes":60,"mustVisit":true,"scheduled":true}
                                  ],
                                  "travelMode":"WALK",
                                  "stadiumEntryBufferMinutes":30,
                                  "stadiumArrivalAt":"2026-09-20T15:00:00",
                                  "gameStartSlackMinutes":180,
                                  "status":"AMPLE",
                                  "warnings":[],
                                  "timeline":[
                                    {"type":"ARRIVAL_TRANSFER","description":"대전역","startsAt":"2026-09-20T12:00:00","endsAt":"2026-09-20T12:00:00"},
                                    {"type":"PLACE_VISIT","description":"수정한 장소","startsAt":"2026-09-20T13:00:00","endsAt":"2026-09-20T14:00:00"},
                                    {"type":"STADIUM_ARRIVAL","description":"대전 한화생명 볼파크","startsAt":"2026-09-20T15:00:00","endsAt":"2026-09-20T15:00:00"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Long.valueOf(id)))
                .andExpect(jsonPath("$.places[0].mustVisit").value(true))
                .andExpect(jsonPath("$.places[0].stayDurationMinutes").value(60));

        mockMvc.perform(get("/api/itineraries/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.travelMode").value("WALK"))
                .andExpect(jsonPath("$.stadiumArrivalAt").value("2026-09-20T15:00:00"))
                .andExpect(jsonPath("$.warnings.length()").value(0))
                .andExpect(jsonPath("$.timeline[1].description").value("수정한 장소"));
    }

    @Test
    void editsOrderedPlacesAndRecalculatesTimelineAndReturnFeasibility() throws Exception {
        String location = mockMvc.perform(post("/api/itineraries/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameId":"game-2026-09-20-001",
                                  "arrivalPlace":"대전역",
                                  "arrivalAt":"2026-09-20T12:00:00",
                                  "departurePlace":"대전역",
                                  "departureAt":"2026-09-21T00:30:00",
                                  "stadiumEntryBufferMinutes":30,
                                  "finalLegToStadiumMinutes":10,
                                  "stadiumToDepartureMinutes":45,
                                  "postGameCrowdBufferMinutes":30,
                                  "boardingBufferMinutes":15,
                                  "places":[
                                    {"placeId":"old-place-1","name":"기존 장소 1","travelFromPreviousMinutes":5,"stayDurationMinutes":10},
                                    {"placeId":"old-place-2","name":"기존 장소 2","travelFromPreviousMinutes":5,"stayDurationMinutes":10}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        String id = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(put("/api/itineraries/{id}/places", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stadiumEntryBufferMinutes":30,
                                  "finalLegToStadiumMinutes":10,
                                  "stadiumToDepartureMinutes":45,
                                  "postGameCrowdBufferMinutes":30,
                                  "boardingBufferMinutes":15,
                                  "places":[
                                    {"placeId":"place-b","name":"두 번째 후보를 앞으로 이동","latitude":36.35,"longitude":127.38,"travelFromPreviousMinutes":10,"stayDurationMinutes":30},
                                    {"placeId":"place-a","name":"추가한 후보","latitude":36.36,"longitude":127.39,"travelFromPreviousMinutes":20,"stayDurationMinutes":60,"mustVisit":true}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(2))
                .andExpect(jsonPath("$.places[0].placeId").value("place-b"))
                .andExpect(jsonPath("$.places[0].travelFromPreviousMinutes").value(10))
                .andExpect(jsonPath("$.places[0].arrivesAt").value("2026-09-20T12:10:00"))
                .andExpect(jsonPath("$.places[1].placeId").value("place-a"))
                .andExpect(jsonPath("$.places[1].arrivesAt").value("2026-09-20T13:00:00"))
                .andExpect(jsonPath("$.stadiumArrivalAt").value("2026-09-20T14:10:00"))
                .andExpect(jsonPath("$.gameStartSlackMinutes").value(260))
                .andExpect(jsonPath("$.stadiumDepartureRecommendedAt").value("2026-09-20T23:00:00"))
                .andExpect(jsonPath("$.returnTransportBoardable").value(true))
                .andExpect(jsonPath("$.status").value("AMPLE"));

        mockMvc.perform(get("/api/itineraries/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places[0].travelFromPreviousMinutes").value(10))
                .andExpect(jsonPath("$.places[0].name").value("두 번째 후보를 앞으로 이동"));
    }

    @Test
    void retainsAnInfeasibleEditAndExplainsLateArrivalAndMissedReturn() throws Exception {
        String location = mockMvc.perform(post("/api/itineraries/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameId":"game-2026-09-20-001",
                                  "arrivalPlace":"대전역",
                                  "arrivalAt":"2026-09-20T17:40:00",
                                  "departurePlace":"대전역",
                                  "departureAt":"2026-09-20T22:00:00",
                                  "stadiumEntryBufferMinutes":30,
                                  "finalLegToStadiumMinutes":10,
                                  "stadiumToDepartureMinutes":60,
                                  "postGameCrowdBufferMinutes":30,
                                  "boardingBufferMinutes":15,
                                  "places":[]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        String id = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(put("/api/itineraries/{id}/places", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stadiumEntryBufferMinutes":30,
                                  "finalLegToStadiumMinutes":10,
                                  "stadiumToDepartureMinutes":60,
                                  "postGameCrowdBufferMinutes":30,
                                  "boardingBufferMinutes":15,
                                  "places":[
                                    {"placeId":"late-place","name":"늦은 방문지","latitude":36.35,"longitude":127.38,"travelFromPreviousMinutes":20,"stayDurationMinutes":50}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stadiumArrivalAt").value("2026-09-20T19:00:00"))
                .andExpect(jsonPath("$.returnTransportBoardable").value(false))
                .andExpect(jsonPath("$.status").value("RETURN_CAUTION"))
                .andExpect(jsonPath("$.warnings[1]").value("입장 여유시간 부족: 경기 지각 가능"))
                .andExpect(jsonPath("$.warnings[2]").value(
                        "예상 경기 종료 후 역 이동·혼잡·탑승 여유시간이 부족해 귀가편 탑승이 어렵습니다."));
    }

    @Test
    void rejectsPlaceEditForUnknownItineraryId() throws Exception {
        mockMvc.perform(put("/api/itineraries/{id}/places", 9223372036854775807L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stadiumEntryBufferMinutes":30,
                                  "finalLegToStadiumMinutes":10,
                                  "stadiumToDepartureMinutes":45,
                                  "postGameCrowdBufferMinutes":30,
                                  "boardingBufferMinutes":15,
                                  "places":[]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ITINERARY_NOT_FOUND"));
    }

    @Test
    void rejectsInvalidPlaceAndTimeValuesInEditRequest() throws Exception {
        mockMvc.perform(put("/api/itineraries/{id}/places", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stadiumEntryBufferMinutes":30,
                                  "finalLegToStadiumMinutes":10,
                                  "stadiumToDepartureMinutes":45,
                                  "postGameCrowdBufferMinutes":30,
                                  "boardingBufferMinutes":15,
                                  "places":[
                                    {"placeId":"","name":"잘못된 장소","latitude":91,"longitude":127,"travelFromPreviousMinutes":-1,"stayDurationMinutes":-5}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details['places[0].placeId']").exists())
                .andExpect(jsonPath("$.details['places[0].latitude']").exists())
                .andExpect(jsonPath("$.details['places[0].travelFromPreviousMinutes']").exists())
                .andExpect(jsonPath("$.details['places[0].stayDurationMinutes']").exists());
    }

    @Test
    void rejectsInvalidDateFormat() throws Exception {
        mockMvc.perform(post("/api/itineraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gameId":"g1","arrivalAt":"20-09-2026"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void rejectsUnknownSelectedGame() throws Exception {
        mockMvc.perform(post("/api/itineraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameId\":\"unknown-game\",\"arrivalPlace\":\"서울역\",\"arrivalAt\":\"2026-09-20T12:00:00\",\"departurePlace\":\"서울역\",\"departureAt\":\"2026-09-20T23:30:00\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/itineraries/9223372036854775807"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ITINERARY_NOT_FOUND"));
    }

    @Test
    void returnsBadRequestForInvalidIdFormat() throws Exception {
        mockMvc.perform(get("/api/itineraries/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void exposesOpenApiDocumentForItineraryEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/itineraries'].post").exists())
                .andExpect(jsonPath("$.paths['/api/itineraries/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/itineraries/{id}/places'].put").exists())
                .andExpect(jsonPath("$.paths['/api/itineraries/{id}'].get").exists());

        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
