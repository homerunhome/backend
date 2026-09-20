package org.zerozero.homerunhome.game.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import org.zerozero.homerunhome.game.domain.Game;

@Schema(description = "야구 경기 응답")
public record GameResponse(
        String gameId,
        String city,
        LocalDate gameDate,
        LocalTime gameStartTime,
        String stadium,
        String homeTeam,
        String awayTeam) {

    public static GameResponse from(Game game) {
        return new GameResponse(
                game.getGameId(),
                game.getCity(),
                game.getGameDate(),
                game.getGameStartTime(),
                game.getStadium(),
                game.getHomeTeam(),
                game.getAwayTeam());
    }
}
