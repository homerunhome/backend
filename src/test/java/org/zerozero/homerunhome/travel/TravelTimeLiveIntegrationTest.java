package org.zerozero.homerunhome.travel;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.zerozero.homerunhome.travel.dto.TravelCoordinate;
import org.zerozero.homerunhome.travel.dto.TravelMode;
import org.zerozero.homerunhome.travel.dto.TravelTimeRequest;
import org.zerozero.homerunhome.travel.service.TravelTimeService;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "KAKAO_REST_API_KEY", matches = "\\S+")
class TravelTimeLiveIntegrationTest {

    @Autowired
    private TravelTimeService service;

    @Test
    void findsDrivingTimeFromJamsilBaseballStadiumToSeoulStation() {
        TravelTimeRequest request = new TravelTimeRequest(
                new TravelCoordinate(new BigDecimal("37.5121"), new BigDecimal("127.0719")),
                new TravelCoordinate(new BigDecimal("37.5547"), new BigDecimal("126.9707")),
                TravelMode.CAR);

        var response = service.findTravelTime(request);

        assertThat(response.durationSeconds()).isPositive();
        assertThat(response.distanceMeters()).isPositive();
    }
}
