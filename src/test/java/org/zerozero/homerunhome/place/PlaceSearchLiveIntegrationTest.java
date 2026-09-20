package org.zerozero.homerunhome.place;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.zerozero.homerunhome.place.dto.MapBounds;
import org.zerozero.homerunhome.place.dto.PlaceCategory;
import org.zerozero.homerunhome.place.dto.PlaceSearchRequest;
import org.zerozero.homerunhome.place.service.PlaceSearchService;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "KAKAO_REST_API_KEY", matches = "\\S+")
class PlaceSearchLiveIntegrationTest {

    @Autowired
    private PlaceSearchService service;

    @Test
    void searchesRealFoodPlacesInsideDaejeonMapBounds() {
        MapBounds daejeonCenter = new MapBounds(
                new BigDecimal("36.30"),
                new BigDecimal("127.38"),
                new BigDecimal("36.38"),
                new BigDecimal("127.48"));

        var response = service.search(
                new PlaceSearchRequest("성심당", PlaceCategory.FOOD, daejeonCenter, 1, 15));

        assertThat(response.places()).isNotEmpty();
        assertThat(response.places()).allSatisfy(place -> {
            assertThat(place.id()).isNotBlank();
            assertThat(place.name()).isNotBlank();
            assertThat(place.latitude()).isNotNull();
            assertThat(place.longitude()).isNotNull();
            assertThat(place.category()).isEqualTo(PlaceCategory.FOOD);
        });
    }
}
