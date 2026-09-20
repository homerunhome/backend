package org.zerozero.homerunhome.itinerary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ItineraryTimelineEntry {

    @Column(name = "timeline_type", length = 40)
    private String type;

    @Column(name = "timeline_description", length = 300)
    private String description;

    @Column(name = "timeline_starts_at")
    private LocalDateTime startsAt;

    @Column(name = "timeline_ends_at")
    private LocalDateTime endsAt;
}
