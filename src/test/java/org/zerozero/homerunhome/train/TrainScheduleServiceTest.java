package org.zerozero.homerunhome.train;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zerozero.homerunhome.train.client.TagoTrainClient;
import org.zerozero.homerunhome.train.dto.TrainCityResponse;
import org.zerozero.homerunhome.train.dto.TrainScheduleResponse;
import org.zerozero.homerunhome.train.dto.TrainStationResponse;
import org.zerozero.homerunhome.train.service.TrainScheduleService;

class TrainScheduleServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 20);

    private TagoTrainClient tagoTrainClient;
    private TrainScheduleService service;

    @BeforeEach
    void setUp() {
        tagoTrainClient = mock(TagoTrainClient.class);
        service = new TrainScheduleService(tagoTrainClient);
        when(tagoTrainClient.findCities()).thenReturn(List.of(new TrainCityResponse("25", "대전광역시")));
        when(tagoTrainClient.findStations("25")).thenReturn(List.of(
                new TrainStationResponse("DAEJEON", "대전", "25"),
                new TrainStationResponse("SEODAEJEON", "서대전", "25")));
        when(tagoTrainClient.findTrainGradeCodes()).thenReturn(List.of("00", "02"));
    }

    @Test
    void returnsAllTrainGradesAndDaejeonDestinationsInDepartureTimeOrder() {
        when(tagoTrainClient.findSchedulesByGradeCode("SEOUL", "DAEJEON", DATE, "00"))
                .thenReturn(List.of(schedule("KTX-2", "KTX", "서울", "대전", 11, 0)));
        when(tagoTrainClient.findSchedulesByGradeCode("SEOUL", "DAEJEON", DATE, "02"))
                .thenReturn(List.of(schedule("MUG-1", "무궁화호", "서울", "대전", 9, 0)));
        when(tagoTrainClient.findSchedulesByGradeCode("SEOUL", "SEODAEJEON", DATE, "00"))
                .thenReturn(List.of(schedule("KTX-1", "KTX", "서울", "서대전", 10, 0)));
        when(tagoTrainClient.findSchedulesByGradeCode("SEOUL", "SEODAEJEON", DATE, "02"))
                .thenReturn(List.of());

        List<TrainScheduleResponse> schedules = service.findSchedulesToDaejeon("SEOUL", DATE);

        assertThat(schedules).extracting(TrainScheduleResponse::trainNumber)
                .containsExactly("MUG-1", "KTX-1", "KTX-2");
        assertThat(schedules).extracting(TrainScheduleResponse::arrivalStation)
                .containsExactly("대전", "서대전", "대전");
        verify(tagoTrainClient).findSchedulesByGradeCode("SEOUL", "DAEJEON", DATE, "00");
        verify(tagoTrainClient).findSchedulesByGradeCode("SEOUL", "DAEJEON", DATE, "02");
        verify(tagoTrainClient).findSchedulesByGradeCode("SEOUL", "SEODAEJEON", DATE, "00");
        verify(tagoTrainClient).findSchedulesByGradeCode("SEOUL", "SEODAEJEON", DATE, "02");
    }

    @Test
    void searchesEveryDaejeonDepartureStationForTheReturnDirection() {
        when(tagoTrainClient.findSchedulesByGradeCode("DAEJEON", "SEOUL", DATE, "00"))
                .thenReturn(List.of(schedule("KTX-1", "KTX", "대전", "서울", 18, 0)));
        when(tagoTrainClient.findSchedulesByGradeCode("DAEJEON", "SEOUL", DATE, "02"))
                .thenReturn(List.of());
        when(tagoTrainClient.findSchedulesByGradeCode("SEODAEJEON", "SEOUL", DATE, "00"))
                .thenReturn(List.of(schedule("KTX-2", "KTX", "서대전", "서울", 17, 0)));
        when(tagoTrainClient.findSchedulesByGradeCode("SEODAEJEON", "SEOUL", DATE, "02"))
                .thenReturn(List.of());

        List<TrainScheduleResponse> schedules = service.findSchedulesFromDaejeon("SEOUL", DATE);

        assertThat(schedules).extracting(TrainScheduleResponse::trainNumber)
                .containsExactly("KTX-2", "KTX-1");
        assertThat(schedules).extracting(TrainScheduleResponse::departureStation)
                .containsExactly("서대전", "대전");
    }

    private TrainScheduleResponse schedule(
            String trainNumber, String trainType, String departureStation, String arrivalStation, int hour, int minute) {
        LocalDateTime departureAt = DATE.atTime(hour, minute);
        return new TrainScheduleResponse(
                trainNumber,
                trainType,
                departureStation,
                departureAt,
                arrivalStation,
                departureAt.plusHours(1));
    }
}
