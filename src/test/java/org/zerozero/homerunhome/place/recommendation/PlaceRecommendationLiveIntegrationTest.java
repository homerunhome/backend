package org.zerozero.homerunhome.place.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.zerozero.homerunhome.place.dto.PlaceCategory;
import org.zerozero.homerunhome.place.explore.dto.PlaceCoordinate;
import org.zerozero.homerunhome.place.recommendation.dto.PlaceRecommendationRequest;
import org.zerozero.homerunhome.place.recommendation.service.PlaceRecommendationService;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "KAKAO_REST_API_KEY", matches = "\\S+")
class PlaceRecommendationLiveIntegrationTest {

    @Autowired
    private PlaceRecommendationService service;

    @Test
    void recommendsRealFoodAndCafePlacesAroundDaejeonBaseballStadium() {
        PlaceRecommendationRequest request = new PlaceRecommendationRequest(
                List.of(PlaceCategory.FOOD, PlaceCategory.CAFE),
                null,
                new PlaceCoordinate(new BigDecimal("36.317067"), new BigDecimal("127.429188")),
                1500,
                null,
                10);

        var response = service.recommend(request);

        assertThat(response.places()).isNotEmpty().hasSizeLessThanOrEqualTo(10);
        assertThat(response.places())
                .allMatch(place -> place.distanceMeters() <= 1500)
                .extracting(place -> place.distanceMeters())
                .isSorted();
        assertThat(response.places()).extracting(place -> place.id()).doesNotHaveDuplicates();
    }
}
