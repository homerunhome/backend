package org.zerozero.homerunhome.place.explore;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.zerozero.homerunhome.place.dto.PlaceCategory;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreRequest;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreScope;
import org.zerozero.homerunhome.place.explore.service.PlaceExploreService;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "KAKAO_REST_API_KEY", matches = "\\S+")
class PlaceExploreLiveIntegrationTest {

    @Autowired
    private PlaceExploreService service;

    @Test
    void findsRealFoodPlacesAroundDaejeonBaseballStadium() {
        PlaceExploreRequest request = new PlaceExploreRequest(
                PlaceExploreScope.STADIUM_NEARBY,
                PlaceCategory.FOOD,
                null,
                null,
                null,
                null,
                1,
                15);

        var response = service.explore(request);

        assertThat(response.places()).isNotEmpty();
        assertThat(response.places())
                .allMatch(place -> place.category() == PlaceCategory.FOOD)
                .allMatch(place -> place.distanceMeters() != null && place.distanceMeters() <= 1500);
    }
}
