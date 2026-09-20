package org.zerozero.homerunhome.itinerary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.zerozero.homerunhome.itinerary.repository.ItineraryRepository;

@SpringBootTest
@ActiveProfiles("test")
class ItineraryApiIntegrationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ItineraryRepository itineraryRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        itineraryRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void createsAndReadsItineraryWithOrderedPlaces() throws Exception {
        String request = """
                {
                  "gameId": "game-2026-09-20-001",
                  "gameDate": "2026-09-20",
                  "gameStartTime": "18:30:00",
                  "stadium": "잠실야구장",
                  "homeTeam": "두산 베어스",
                  "awayTeam": "롯데 자이언츠",
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
                .andExpect(jsonPath("$.arrivalPlace").value("서울역"))
                .andExpect(jsonPath("$.preferences[0]").value("FOOD"))
                .andExpect(jsonPath("$.places.length()").value(2))
                .andExpect(jsonPath("$.places[0].name").value("첫 번째 장소"))
                .andExpect(jsonPath("$.places[1].name").value("두 번째 장소"));
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
    void rejectsInvalidDateFormat() throws Exception {
        mockMvc.perform(post("/api/itineraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gameId":"g1","gameDate":"20-09-2026"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
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
}
