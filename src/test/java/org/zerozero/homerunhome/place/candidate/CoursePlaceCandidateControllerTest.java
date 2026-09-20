package org.zerozero.homerunhome.place.candidate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zerozero.homerunhome.common.GlobalExceptionHandler;
import org.zerozero.homerunhome.place.candidate.controller.CoursePlaceCandidateController;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCandidate;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCandidateRequest;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCandidateResponse;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCategory;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceProvider;
import org.zerozero.homerunhome.place.candidate.service.CoursePlaceCandidateService;
import org.zerozero.homerunhome.place.explore.dto.PlaceCoordinate;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreScope;

class CoursePlaceCandidateControllerTest {

    private CoursePlaceCandidateService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(CoursePlaceCandidateService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CoursePlaceCandidateController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsUnifiedCoursePlaceCandidates() throws Exception {
        PlaceCoordinate center = new PlaceCoordinate(
                new BigDecimal("36.317067"), new BigDecimal("127.429188"));
        CoursePlaceCandidateRequest request = new CoursePlaceCandidateRequest(
                PlaceExploreScope.STADIUM_NEARBY,
                List.of(CoursePlaceCategory.FOOD, CoursePlaceCategory.TOURIST_ATTRACTION),
                null,
                null,
                null,
                1500,
                null,
                30);
        when(service.findCandidates(request)).thenReturn(new CoursePlaceCandidateResponse(
                request.scope(),
                center,
                1500,
                List.of(new CoursePlaceCandidate(
                        CoursePlaceProvider.KAKAO,
                        "place-1",
                        "추천 맛집",
                        CoursePlaceCategory.FOOD,
                        "대전 중구",
                        new BigDecimal("36.318"),
                        new BigDecimal("127.430"),
                        null,
                        "https://place.map.kakao.com/place-1",
                        130))));

        mockMvc.perform(post("/api/course-place-candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scope":"STADIUM_NEARBY",
                                  "categories":["FOOD","TOURIST_ATTRACTION"],
                                  "maxDistanceMeters":1500,
                                  "limit":30
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("STADIUM_NEARBY"))
                .andExpect(jsonPath("$.places[0].provider").value("KAKAO"))
                .andExpect(jsonPath("$.places[0].externalId").value("place-1"))
                .andExpect(jsonPath("$.places[0].distanceMeters").value(130));
    }
}
