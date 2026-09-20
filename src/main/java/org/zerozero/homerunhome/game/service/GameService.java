package org.zerozero.homerunhome.game.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
}
