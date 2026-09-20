package org.zerozero.homerunhome.tourism;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.common.GlobalExceptionHandler;
import org.zerozero.homerunhome.tourism.controller.TourismController;
import org.zerozero.homerunhome.tourism.dto.TourismContentListResponse;
import org.zerozero.homerunhome.tourism.dto.TourismContentSummaryResponse;
import org.zerozero.homerunhome.tourism.dto.TourismContentType;
import org.zerozero.homerunhome.tourism.dto.TourismEventListResponse;
import org.zerozero.homerunhome.tourism.service.TourismContentService;
import org.zerozero.homerunhome.tourism.service.TourismEventService;

class TourismControllerTest {

    private TourismContentService contentService;
    private TourismEventService eventService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        contentService = mock(TourismContentService.class);
        eventService = mock(TourismEventService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TourismController(contentService, eventService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsDaejeonTourismContents() throws Exception {
        when(contentService.findDaejeonContents(TourismContentType.TOURIST_ATTRACTION, 1, 12))
                .thenReturn(new TourismContentListResponse(
                        TourismContentType.TOURIST_ATTRACTION,
                        List.of(new TourismContentSummaryResponse(
                                "125407",
                                TourismContentType.TOURIST_ATTRACTION,
                                "한밭수목원",
                                "대전 서구",
                                "",
                                new BigDecimal("36.366"),
                                new BigDecimal("127.388"),
                                "",
                                "https://example.com/image.jpg",
                                "")),
                        1,
                        12,
                        1,
                        false));

        mockMvc.perform(get("/api/tourism/contents")
                        .queryParam("type", "TOURIST_ATTRACTION")
                        .queryParam("page", "1")
                        .queryParam("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentType").value("TOURIST_ATTRACTION"))
                .andExpect(jsonPath("$.contents[0].contentId").value("125407"))
                .andExpect(jsonPath("$.contents[0].title").value("한밭수목원"));
    }

    @Test
    void returnsEventsForDateRange() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 20);
        when(eventService.findDaejeonEvents(date, date, 1, 12))
                .thenReturn(new TourismEventListResponse(date, date, List.of(), 1, 12, 0, false));

        mockMvc.perform(get("/api/tourism/events")
                        .queryParam("startDate", "2026-09-20")
                        .queryParam("endDate", "2026-09-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2026-09-20"))
                .andExpect(jsonPath("$.events").isArray());
    }

    @Test
    void returnsTourismValidationError() throws Exception {
        when(contentService.findDaejeonContents(TourismContentType.UNKNOWN, 1, 12))
                .thenThrow(new BusinessException(ErrorCode.INVALID_TOURISM_REQUEST));

        mockMvc.perform(get("/api/tourism/contents").queryParam("type", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TOURISM_REQUEST"));
    }
}
