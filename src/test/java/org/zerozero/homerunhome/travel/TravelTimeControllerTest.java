package org.zerozero.homerunhome.travel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.common.GlobalExceptionHandler;
import org.zerozero.homerunhome.travel.controller.TravelTimeController;
import org.zerozero.homerunhome.travel.dto.TravelCoordinate;
import org.zerozero.homerunhome.travel.dto.TravelMode;
import org.zerozero.homerunhome.travel.dto.TravelTimeRequest;
import org.zerozero.homerunhome.travel.dto.TravelTimeResponse;
import org.zerozero.homerunhome.travel.service.TravelTimeService;

class TravelTimeControllerTest {

    private TravelTimeService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(TravelTimeService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TravelTimeController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsTravelTimeAndDistance() throws Exception {
        TravelTimeRequest request = request(TravelMode.CAR);
        when(service.findTravelTime(request))
                .thenReturn(new TravelTimeResponse(TravelMode.CAR, 1280, 9400));

        mockMvc.perform(post("/api/travel-times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "origin":{"latitude":37.5121,"longitude":127.0719},
                                  "destination":{"latitude":37.4982,"longitude":127.0276},
                                  "transportMode":"CAR"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transportMode").value("CAR"))
                .andExpect(jsonPath("$.durationSeconds").value(1280))
                .andExpect(jsonPath("$.distanceMeters").value(9400));
    }

    @Test
    void returnsPublicTransitTimeAndDistance() throws Exception {
        TravelTimeRequest request = request(TravelMode.PUBLIC_TRANSIT);
        when(service.findTravelTime(request))
                .thenReturn(new TravelTimeResponse(TravelMode.PUBLIC_TRANSIT, 2115, 5013));

        mockMvc.perform(post("/api/travel-times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "origin":{"latitude":37.5121,"longitude":127.0719},
                                  "destination":{"latitude":37.4982,"longitude":127.0276},
                                  "transportMode":"PUBLIC_TRANSIT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transportMode").value("PUBLIC_TRANSIT"))
                .andExpect(jsonPath("$.durationSeconds").value(2115))
                .andExpect(jsonPath("$.distanceMeters").value(5013));
    }

    @Test
    void returnsUnsupportedTravelModeSeparately() throws Exception {
        TravelTimeRequest request = request(TravelMode.BICYCLE);
        when(service.findTravelTime(request))
                .thenThrow(new BusinessException(ErrorCode.UNSUPPORTED_TRAVEL_MODE));

        mockMvc.perform(post("/api/travel-times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "origin":{"latitude":37.5121,"longitude":127.0719},
                                  "destination":{"latitude":37.4982,"longitude":127.0276},
                                  "transportMode":"BICYCLE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_TRAVEL_MODE"));
    }

    @Test
    void returnsRouteNotFoundSeparately() throws Exception {
        TravelTimeRequest request = request(TravelMode.CAR);
        when(service.findTravelTime(request))
                .thenThrow(new BusinessException(ErrorCode.ROUTE_NOT_FOUND));

        mockMvc.perform(post("/api/travel-times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "origin":{"latitude":37.5121,"longitude":127.0719},
                                  "destination":{"latitude":37.4982,"longitude":127.0276},
                                  "transportMode":"CAR"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROUTE_NOT_FOUND"));
    }

    private TravelTimeRequest request(TravelMode mode) {
        return new TravelTimeRequest(
                new TravelCoordinate(new BigDecimal("37.5121"), new BigDecimal("127.0719")),
                new TravelCoordinate(new BigDecimal("37.4982"), new BigDecimal("127.0276")),
                mode);
    }
}
