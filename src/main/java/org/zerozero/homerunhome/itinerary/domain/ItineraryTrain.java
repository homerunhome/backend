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
public class ItineraryTrain {

    @Column(name = "train_number", length = 20)
    private String trainNumber;

    @Column(name = "train_type", length = 30)
    private String trainType;

    @Column(name = "departure_station", length = 100)
    private String departureStation;

    @Column(name = "departure_at")
    private LocalDateTime departureAt;

    @Column(name = "arrival_station", length = 100)
    private String arrivalStation;

    @Column(name = "arrival_at")
    private LocalDateTime arrivalAt;
}
