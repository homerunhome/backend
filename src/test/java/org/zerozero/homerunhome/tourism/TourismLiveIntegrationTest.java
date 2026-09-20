package org.zerozero.homerunhome.tourism;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.zerozero.homerunhome.tourism.dto.TourismContentType;
import org.zerozero.homerunhome.tourism.service.TourismContentService;
import org.zerozero.homerunhome.tourism.service.TourismEventService;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "TOUR_API_SERVICE_KEY", matches = "\\S+")
class TourismLiveIntegrationTest {

    @Autowired
    private TourismContentService contentService;

    @Autowired
    private TourismEventService eventService;

    @Test
    void findsRealDaejeonTourismContentDetailAndEvents() {
        var contents = contentService.findDaejeonContents(
                TourismContentType.TOURIST_ATTRACTION, 1, 1);

        assertThat(contents.contents()).isNotEmpty();
        var firstContent = contents.contents().getFirst();
        var detail = contentService.findDetail(firstContent.contentId());
        assertThat(detail.contentId()).isEqualTo(firstContent.contentId());
        assertThat(detail.title()).isNotBlank();

        LocalDate today = LocalDate.now();
        var events = eventService.findDaejeonEvents(today, today.plusDays(7), 1, 10);
        assertThat(events.events()).isNotNull();
    }
}
