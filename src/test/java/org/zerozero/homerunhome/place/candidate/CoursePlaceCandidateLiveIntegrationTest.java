package org.zerozero.homerunhome.place.candidate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCandidateRequest;
import org.zerozero.homerunhome.place.candidate.dto.CoursePlaceCategory;
import org.zerozero.homerunhome.place.candidate.service.CoursePlaceCandidateService;
import org.zerozero.homerunhome.place.explore.dto.PlaceExploreScope;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "KAKAO_REST_API_KEY", matches = "\\S+")
@EnabledIfEnvironmentVariable(named = "TOUR_API_SERVICE_KEY", matches = "\\S+")
class CoursePlaceCandidateLiveIntegrationTest {

    @Autowired
    private CoursePlaceCandidateService service;

    @Test
    void findsRealPlacesAroundDaejeonBaseballStadiumFromBothProviders() {
        CoursePlaceCandidateRequest request = new CoursePlaceCandidateRequest(
                PlaceExploreScope.STADIUM_NEARBY,
                List.of(CoursePlaceCategory.FOOD, CoursePlaceCategory.TOURIST_ATTRACTION),
                null,
                null,
                null,
                3000,
                null,
                30);

        var response = service.findCandidates(request);

        assertThat(response.places()).isNotEmpty().hasSizeLessThanOrEqualTo(30);
        assertThat(response.places())
                .allMatch(place -> place.distanceMeters() <= 3000)
                .extracting(place -> place.distanceMeters())
                .isSorted();
    }
}
