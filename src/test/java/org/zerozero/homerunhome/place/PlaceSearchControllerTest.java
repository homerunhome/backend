package org.zerozero.homerunhome.place;

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
import org.zerozero.homerunhome.place.controller.PlaceSearchController;
import org.zerozero.homerunhome.place.dto.PlaceCategory;
import org.zerozero.homerunhome.place.dto.PlaceResponse;
import org.zerozero.homerunhome.place.dto.PlaceSearchRequest;
import org.zerozero.homerunhome.place.dto.PlaceSearchResponse;
import org.zerozero.homerunhome.place.service.PlaceSearchService;

class PlaceSearchControllerTest {

    private PlaceSearchService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(PlaceSearchService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PlaceSearchController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsPlacesAndPaginationMetadata() throws Exception {
        PlaceSearchRequest request = new PlaceSearchRequest("성심당", PlaceCategory.FOOD, null, 1, 15);
        when(service.search(request)).thenReturn(new PlaceSearchResponse(
                List.of(new PlaceResponse(
                        "place-1",
                        "성심당 본점",
                        "대전 중구 은행동 145",
                        "대전 중구 대종로480번길 15",
                        new BigDecimal("36.327681"),
                        new BigDecimal("127.427314"),
                        PlaceCategory.FOOD,
                        "음식점 > 제과,베이커리",
                        "1588-8069",
                        "https://place.map.kakao.com/place-1",
                        null)),
                1,
                15,
                24,
                24,
                true));

        mockMvc.perform(post("/api/places/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"성심당\",\"category\":\"FOOD\",\"page\":1,\"size\":15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places[0].id").value("place-1"))
                .andExpect(jsonPath("$.places[0].latitude").value(36.327681))
                .andExpect(jsonPath("$.places[0].longitude").value(127.427314))
                .andExpect(jsonPath("$.places[0].category").value("FOOD"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void returnsAnEmptyListAsAValidSearchResult() throws Exception {
        PlaceSearchRequest request = new PlaceSearchRequest("없는 장소", null, null, 1, 15);
        when(service.search(request)).thenReturn(new PlaceSearchResponse(List.of(), 1, 15, 0, 0, false));

        mockMvc.perform(post("/api/places/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"없는 장소\",\"page\":1,\"size\":15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places").isEmpty())
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void returnsExternalApiFailureSeparately() throws Exception {
        PlaceSearchRequest request = new PlaceSearchRequest("카페", null, null, 1, 15);
        when(service.search(request))
                .thenThrow(new BusinessException(ErrorCode.KAKAO_EXTERNAL_SERVER_ERROR));

        mockMvc.perform(post("/api/places/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"카페\",\"page\":1,\"size\":15}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("KAKAO_EXTERNAL_SERVER_ERROR"));
    }
}
