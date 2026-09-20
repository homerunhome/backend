package org.zerozero.homerunhome.train.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zerozero.homerunhome.train.client.TagoTrainClient;
import org.zerozero.homerunhome.train.dto.TrainCityResponse;
import org.zerozero.homerunhome.train.dto.TrainScheduleResponse;
import org.zerozero.homerunhome.train.dto.TrainStationResponse;

@Service
@RequiredArgsConstructor
public class TrainScheduleService {

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
}
