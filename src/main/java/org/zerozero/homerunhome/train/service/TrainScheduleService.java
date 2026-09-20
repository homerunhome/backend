package org.zerozero.homerunhome.train.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.train.client.TagoTrainClient;
import org.zerozero.homerunhome.train.dto.TrainCityResponse;
import org.zerozero.homerunhome.train.dto.TrainScheduleResponse;
import org.zerozero.homerunhome.train.dto.TrainStationResponse;

@Service
@RequiredArgsConstructor
public class TrainScheduleService {

    private static final Logger log = LoggerFactory.getLogger(TrainScheduleService.class);

    private final TagoTrainClient tagoTrainClient;

    public List<TrainCityResponse> findCities() {
        return tagoTrainClient.findCities();
    }

    public List<TrainStationResponse> findStations(String cityCode) {
        return tagoTrainClient.findStations(cityCode);
    }

    public List<TrainScheduleResponse> findKtxSchedules(
            String departureStationId, String arrivalStationId, LocalDate date) {
        return tagoTrainClient.findKtxSchedules(departureStationId, arrivalStationId, date);
    }

    public List<TrainScheduleResponse> findSchedulesToDaejeon(String departureStationId, LocalDate date) {
        return findSchedulesAcrossDaejeonStations(departureStationId, date, true);
    }

    public List<TrainScheduleResponse> findSchedulesFromDaejeon(String arrivalStationId, LocalDate date) {
        return findSchedulesAcrossDaejeonStations(arrivalStationId, date, false);
    }

    private List<TrainScheduleResponse> findSchedulesAcrossDaejeonStations(
            String outsideStationId, LocalDate date, boolean arrivingInDaejeon) {
        String daejeonCityCode = tagoTrainClient.findCities().stream()
                .filter(city -> city.cityName().contains("대전"))
                .map(TrainCityResponse::cityCode)
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("TAGO city list did not contain a Daejeon entry");
                    return new BusinessException(ErrorCode.TRAIN_SCHEDULE_PROVIDER_ERROR);
                });

        List<String> daejeonStationIds = tagoTrainClient.findStations(daejeonCityCode).stream()
                .map(TrainStationResponse::stationId)
                .filter(id -> !id.isBlank())
                .distinct()
                .toList();
        List<String> trainGradeCodes = tagoTrainClient.findTrainGradeCodes();

        List<TrainScheduleResponse> schedules = new ArrayList<>();
        for (String daejeonStationId : daejeonStationIds) {
            for (String gradeCode : trainGradeCodes) {
                String departureStationId = arrivingInDaejeon ? outsideStationId : daejeonStationId;
                String arrivalStationId = arrivingInDaejeon ? daejeonStationId : outsideStationId;
                schedules.addAll(tagoTrainClient.findSchedulesByGradeCode(
                        departureStationId, arrivalStationId, date, gradeCode));
            }
        }

        return schedules.stream()
                .distinct()
                .sorted(Comparator.comparing(TrainScheduleResponse::departureAt)
                        .thenComparing(TrainScheduleResponse::trainType)
                        .thenComparing(TrainScheduleResponse::trainNumber))
                .toList();
    }
}
