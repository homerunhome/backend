package org.zerozero.homerunhome.game.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "games")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game {

    @Id
    @Column(name = "game_id", nullable = false, updatable = false, length = 100)
    private String gameId;

    @Column(nullable = false, length = 100)
    private String city;

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

    @Builder
    public Game(
            String gameId,
            String city,
            LocalDate gameDate,
            LocalTime gameStartTime,
            String stadium,
            String homeTeam,
            String awayTeam) {
        this.gameId = gameId;
        this.city = city;
        this.gameDate = gameDate;
        this.gameStartTime = gameStartTime;
        this.stadium = stadium;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
    }
}
