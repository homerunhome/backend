package org.zerozero.homerunhome.itinerary.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.zerozero.homerunhome.game.domain.Game;

@Entity
@Table(name = "itineraries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "itinerary_id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

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
            Game game,
            String arrivalPlace,
            LocalDateTime arrivalAt,
            String departurePlace,
            LocalDateTime departureAt,
            List<String> preferences,
            List<ItineraryPlace> places) {
        this.game = game;
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
