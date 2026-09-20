package org.zerozero.homerunhome.game.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerozero.homerunhome.common.BusinessException;
import org.zerozero.homerunhome.common.ErrorCode;
import org.zerozero.homerunhome.game.domain.Game;
import org.zerozero.homerunhome.game.dto.GameRegistrationRequest;
import org.zerozero.homerunhome.game.dto.GameResponse;
import org.zerozero.homerunhome.game.repository.GameRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;

    public List<GameResponse> getGames() {
        return gameRepository.findAllByOrderByGameDateAscGameStartTimeAsc().stream()
                .map(GameResponse::from)
                .toList();
    }

    public GameResponse getGame(String gameId) {
        return gameRepository.findById(gameId)
                .map(GameResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));
    }

    @Transactional
    public GameResponse register(GameRegistrationRequest request) {
        if (gameRepository.existsById(request.gameId())) {
            throw new BusinessException(ErrorCode.GAME_ALREADY_EXISTS);
        }
        Game game = Game.builder()
                .gameId(request.gameId())
                .city(request.city())
                .gameDate(request.gameDate())
                .gameStartTime(request.gameStartTime())
                .stadium(request.stadium())
                .homeTeam(request.homeTeam())
                .awayTeam(request.awayTeam())
                .build();
        return GameResponse.from(gameRepository.save(game));
    }
}
