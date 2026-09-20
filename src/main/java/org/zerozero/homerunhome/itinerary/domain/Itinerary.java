package org.zerozero.homerunhome.itinerary.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "itineraries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "itinerary_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "game_id", nullable = false, length = 100)
    private String gameId;

    @Column(name = "game_date", nullable = false)
    private LocalDate gameDate;

    @Column(name = "game_start_time", nullable = false)
    private LocalTime gameStartTime;

    @Column(nullable = false, length = 150)
    private String stadium;

    @Column(name = "home_team", nullable = false, length = 100)
    private String homeTeam;

    @Column(name = "away_team", nullable = false, length = 100)
    private String awayTeam;

    @Column(name = "arrival_place", nullable = false, length = 200)
    private String arrivalPlace;

    @Column(name = "arrival_at", nullable = false)
    private LocalDateTime arrivalAt;

    @Column(name = "departure_place", nullable = false, length = 200)
    private String departurePlace;

    @Column(name = "departure_at", nullable = false)
    private LocalDateTime departureAt;

    @ElementCollection
    @CollectionTable(name = "itinerary_preferences", joinColumns = @JoinColumn(name = "itinerary_id"))
    @Column(name = "preference", nullable = false, length = 50)
    @OrderColumn(name = "preference_order")
    @Getter(AccessLevel.NONE)
    private List<String> preferences = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "itinerary_places", joinColumns = @JoinColumn(name = "itinerary_id"))
    @OrderColumn(name = "place_order")
    @Getter(AccessLevel.NONE)
    private List<ItineraryPlace> places = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Builder
    public Itinerary(
            String gameId,
            LocalDate gameDate,
            LocalTime gameStartTime,
            String stadium,
            String homeTeam,
            String awayTeam,
            String arrivalPlace,
            LocalDateTime arrivalAt,
            String departurePlace,
            LocalDateTime departureAt,
            List<String> preferences,
            List<ItineraryPlace> places) {
        this.gameId = gameId;
        this.gameDate = gameDate;
        this.gameStartTime = gameStartTime;
        this.stadium = stadium;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.arrivalPlace = arrivalPlace;
        this.arrivalAt = arrivalAt;
        this.departurePlace = departurePlace;
        this.departureAt = departureAt;
        this.preferences = new ArrayList<>(preferences);
        this.places = new ArrayList<>(places);
    }

    public List<String> getPreferences() {
        return List.copyOf(preferences);
    }

    public List<ItineraryPlace> getPlaces() {
        return List.copyOf(places);
    }
}
