package org.zerozero.homerunhome.place.explore;

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
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.common.GlobalExceptionHandler;
import org.zerozero.homerunhome.place.dto.PlaceCategory;
import org.zerozero.homerunhome.place.dto.PlaceResponse;
import org.zerozero.homerunhome.place.explore.controller.PlaceExploreController;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreRequest;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreResponse;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreScope;
import org.zerozero.homerunhome.place.explore.service.PlaceExploreService;

class PlaceExploreControllerTest {

    private PlaceExploreService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(PlaceExploreService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PlaceExploreController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsPlacesForSelectedScopeAndCategory() throws Exception {
        PlaceExploreRequest request = new PlaceExploreRequest(
                PlaceExploreScope.STADIUM_NEARBY,
                PlaceCategory.FOOD,
                null,
                null,
                null,
                null,
                1,
                15);
        when(service.explore(request)).thenReturn(new PlaceExploreResponse(
                PlaceExploreScope.STADIUM_NEARBY,
                null,
                List.of(new PlaceResponse(
                        "place-1",
                        "야구장 근처 식당",
                        "대전 중구 부사동 1",
                        "대전 중구 대종로 1",
                        new BigDecimal("36.32"),
                        new BigDecimal("127.43"),
                        PlaceCategory.FOOD,
                        "음식점 > 한식",
                        "042-123-4567",
                        "https://place.map.kakao.com/place-1",
                        320)),
                1,
                15,
                1,
                1,
                false));

        mockMvc.perform(post("/api/places/explore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scope":"STADIUM_NEARBY",
                                  "category":"FOOD",
                                  "page":1,
                                  "size":15
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("STADIUM_NEARBY"))
                .andExpect(jsonPath("$.places[0].id").value("place-1"))
                .andExpect(jsonPath("$.places[0].distanceMeters").value(320))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void returnsInvalidExplorationConditionSeparately() throws Exception {
        PlaceExploreRequest request = new PlaceExploreRequest(
                PlaceExploreScope.CURRENT_LOCATION,
                PlaceCategory.CAFE,
                null,
                null,
                null,
                null,
                null,
                null);
        when(service.explore(request))
                .thenThrow(new BusinessException(ErrorCode.INVALID_PLACE_EXPLORATION_REQUEST));

        mockMvc.perform(post("/api/places/explore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scope":"CURRENT_LOCATION","category":"CAFE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PLACE_EXPLORATION_REQUEST"));
    }

    @Test
    void returnsExternalApiFailureSeparately() throws Exception {
        PlaceExploreRequest request = new PlaceExploreRequest(
                PlaceExploreScope.STATION_NEARBY,
                PlaceCategory.CONVENIENCE_STORE,
                null,
                null,
                null,
                null,
                null,
                null);
        when(service.explore(request))
                .thenThrow(new BusinessException(ErrorCode.KAKAO_EXTERNAL_SERVER_ERROR));

        mockMvc.perform(post("/api/places/explore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scope":"STATION_NEARBY","category":"CONVENIENCE_STORE"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("KAKAO_EXTERNAL_SERVER_ERROR"));
    }
}
