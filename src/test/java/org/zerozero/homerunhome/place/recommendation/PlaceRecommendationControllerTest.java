package org.zerozero.homerunhome.place.recommendation;

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
import org.zerozero.homerunhome.place.explore.dto.PlaceCoordinate;
import org.zerozero.homerunhome.place.recommendation.controller.PlaceRecommendationController;
import org.zerozero.homerunhome.place.recommendation.dto.PlaceRecommendationRequest;
import org.zerozero.homerunhome.place.recommendation.dto.PlaceRecommendationResponse;
import org.zerozero.homerunhome.place.recommendation.service.PlaceRecommendationService;

class PlaceRecommendationControllerTest {

    private PlaceRecommendationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(PlaceRecommendationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PlaceRecommendationController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsRecommendedPlaces() throws Exception {
        PlaceCoordinate referencePoint = new PlaceCoordinate(
                new BigDecimal("36.317067"), new BigDecimal("127.429188"));
        PlaceRecommendationRequest request = new PlaceRecommendationRequest(
                List.of(PlaceCategory.FOOD, PlaceCategory.CAFE),
                "디저트",
                referencePoint,
                1500,
                null,
                10);
        when(service.recommend(request)).thenReturn(new PlaceRecommendationResponse(
                request.categories(),
                request.keyword(),
                referencePoint,
                1500,
                List.of(new PlaceResponse(
                        "place-1",
                        "추천 카페",
                        "대전 중구",
                        "대전 중구 도로명",
                        new BigDecimal("36.318"),
                        new BigDecimal("127.430"),
                        PlaceCategory.CAFE,
                        "카페",
                        "042-123-4567",
                        "https://place.map.kakao.com/place-1",
                        130))));

        mockMvc.perform(post("/api/places/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categories":["FOOD","CAFE"],
                                  "keyword":"디저트",
                                  "referencePoint":{"latitude":36.317067,"longitude":127.429188},
                                  "maxDistanceMeters":1500,
                                  "limit":10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0]").value("FOOD"))
                .andExpect(jsonPath("$.places[0].id").value("place-1"))
                .andExpect(jsonPath("$.places[0].distanceMeters").value(130));
    }

    @Test
    void returnsInvalidRecommendationConditionSeparately() throws Exception {
        PlaceRecommendationRequest request = new PlaceRecommendationRequest(
                List.of(PlaceCategory.FOOD), null, null, 1500, null, 10);
        when(service.recommend(request))
                .thenThrow(new BusinessException(ErrorCode.INVALID_PLACE_RECOMMENDATION_REQUEST));

        mockMvc.perform(post("/api/places/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categories":["FOOD"],"maxDistanceMeters":1500,"limit":10}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PLACE_RECOMMENDATION_REQUEST"));
    }
}
