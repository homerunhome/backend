package org.zerozero.homerunhome.tourapi.client;

import static org.zerozero.homerunhome.tourapi.client.TourApiFieldReader.text;

import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.zerozero.homerunhome.tourapi.dto.TourApiAccessibility;

@Component
public class BarrierFreeTourApiClient {

    private final RestClient restClient;
    private final TourApiClientExecutor executor;

    public BarrierFreeTourApiClient(
            @Qualifier("barrierFreeTourRestClient") RestClient restClient,
            TourApiClientExecutor executor) {
        this.restClient = restClient;
        this.executor = executor;
    }

    public Optional<TourApiAccessibility> findDetail(String contentId) {
        TourApiPage response = executor.execute(
                restClient, "detailWithTour2", Map.of("contentId", contentId), 1, 1);
        return response.items().stream().findFirst().map(this::toAccessibility);
    }

    private TourApiAccessibility toAccessibility(Map<String, Object> fields) {
        return new TourApiAccessibility(
                text(fields, "parking"),
                text(fields, "route"),
                text(fields, "publictransport"),
                text(fields, "ticketoffice"),
                text(fields, "wheelchair"),
                text(fields, "exit"),
                text(fields, "elevator"),
                text(fields, "restroom"),
                text(fields, "auditorium"),
                text(fields, "room"),
                text(fields, "handicapetc"),
                text(fields, "braileblock"),
                text(fields, "helpdog"),
                text(fields, "guidehuman"),
                text(fields, "audioguide"),
                text(fields, "bigprint"),
                text(fields, "brailepromotion"),
                text(fields, "guidesystem"),
                text(fields, "blindhandicapetc"),
                text(fields, "signguide"),
                text(fields, "videoguide"),
                text(fields, "hearingroom"),
                text(fields, "hearinghandicapetc"),
                text(fields, "stroller"),
                text(fields, "lactationroom"),
                text(fields, "babysparechair"),
                text(fields, "infantsfamilyetc"));
    }
}
