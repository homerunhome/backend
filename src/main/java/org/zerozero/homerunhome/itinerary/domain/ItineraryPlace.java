package org.zerozero.homerunhome.itinerary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ItineraryPlace {

    @Column(name = "place_id", length = 100)
    private String placeId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "travel_from_previous_minutes")
    private Long travelFromPreviousMinutes;

    @Column(name = "arrives_at")
    private LocalDateTime arrivesAt;

    @Column(name = "departs_at")
    private LocalDateTime departsAt;

    @Column(name = "stay_duration_minutes")
    private Long stayDurationMinutes;

    @Column(name = "must_visit")
    private Boolean mustVisit;

    @Column(name = "place_scheduled")
    private Boolean scheduled;

}
