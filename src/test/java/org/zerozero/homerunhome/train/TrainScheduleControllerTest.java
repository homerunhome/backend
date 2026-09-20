package org.zerozero.homerunhome.train;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zerozero.homerunhome.train.controller.TrainScheduleController;
import org.zerozero.homerunhome.train.dto.TrainCityResponse;
import org.zerozero.homerunhome.train.dto.TrainScheduleResponse;
import org.zerozero.homerunhome.train.dto.TrainStationResponse;
import org.zerozero.homerunhome.train.service.TrainScheduleService;

class TrainScheduleControllerTest {

    private TrainScheduleService trainScheduleService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        trainScheduleService = mock(TrainScheduleService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TrainScheduleController(trainScheduleService)).build();
    }

    @Test
    void searchesKtxSchedulesForRouteAndDate() throws Exception {
        when(trainScheduleService.findKtxSchedules("NAT010000", "NAT011668", LocalDate.of(2026, 9, 20)))
                .thenReturn(List.of(new TrainScheduleResponse(
                        "001",
                        "KTX",
                        "서울",
                        LocalDateTime.of(2026, 9, 20, 8, 0),
                        "대전",
                        LocalDateTime.of(2026, 9, 20, 9, 0))));

        mockMvc.perform(get("/api/trains/ktx")
                        .param("departureStationId", "NAT010000")
                        .param("arrivalStationId", "NAT011668")
                        .param("date", "2026-09-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainNumber").value("001"))
                .andExpect(jsonPath("$[0].departureStation").value("서울"))
                .andExpect(jsonPath("$[0].arrivalStation").value("대전"));

        verify(trainScheduleService).findKtxSchedules("NAT010000", "NAT011668", LocalDate.of(2026, 9, 20));
    }

    @Test
    void listsCityAndStationOptions() throws Exception {
        when(trainScheduleService.findCities()).thenReturn(List.of(new TrainCityResponse("11", "서울특별시")));
        when(trainScheduleService.findStations("11"))
                .thenReturn(List.of(new TrainStationResponse("NAT010000", "서울", "11")));

        mockMvc.perform(get("/api/trains/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cityCode").value("11"));
        mockMvc.perform(get("/api/trains/stations").param("cityCode", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stationId").value("NAT010000"));

        verify(trainScheduleService).findCities();
        verify(trainScheduleService).findStations("11");
    }
}
