package org.zerozero.homerunhome.geocoding;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import org.zerozero.homerunhome.geocoding.controller.AddressGeocodingController;
import org.zerozero.homerunhome.geocoding.dto.AddressCoordinateResponse;
import org.zerozero.homerunhome.geocoding.service.AddressGeocodingService;

class AddressGeocodingControllerTest {

    private AddressGeocodingService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AddressGeocodingService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AddressGeocodingController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsLatitudeAndLongitudeWithClearFieldNames() throws Exception {
        when(service.geocode("대전 중구 대종로 373"))
                .thenReturn(new AddressCoordinateResponse(
                        "대전 중구 부사동 209-1",
                        "대전 중구 대종로 373",
                        new BigDecimal("36.317067"),
                        new BigDecimal("127.429188")));

        mockMvc.perform(post("/api/geocoding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"대전 중구 대종로 373\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(36.317067))
                .andExpect(jsonPath("$.longitude").value(127.429188))
                .andExpect(jsonPath("$.roadAddressName").value("대전 중구 대종로 373"));

        verify(service).geocode("대전 중구 대종로 373");
    }

    @Test
    void returnsNotFoundErrorWhenAddressCannotBeFound() throws Exception {
        when(service.geocode("없는 주소"))
                .thenThrow(new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        mockMvc.perform(post("/api/geocoding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"없는 주소\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ADDRESS_NOT_FOUND"));
    }

    @Test
    void returnsBadRequestWhenAddressIsMissing() throws Exception {
        when(service.geocode(null)).thenThrow(new BusinessException(ErrorCode.ADDRESS_REQUIRED));

        mockMvc.perform(post("/api/geocoding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ADDRESS_REQUIRED"));
    }
}
