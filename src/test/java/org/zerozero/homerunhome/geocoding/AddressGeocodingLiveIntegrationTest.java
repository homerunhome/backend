package org.zerozero.homerunhome.geocoding;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.zerozero.homerunhome.geocoding.service.AddressGeocodingService;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "KAKAO_REST_API_KEY", matches = "\\S+")
class AddressGeocodingLiveIntegrationTest {

    @Autowired
    private AddressGeocodingService service;

    @Test
    void convertsARealStadiumRoadAddress() {
        var response = service.geocode("대전 중구 대종로 373");

        assertThat(response.roadAddressName()).isEqualTo("대전 중구 대종로 373");
        assertThat(response.latitude()).isBetween(new BigDecimal("36.0"), new BigDecimal("37.0"));
        assertThat(response.longitude()).isBetween(new BigDecimal("127.0"), new BigDecimal("128.0"));
    }
}
