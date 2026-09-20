package org.zerozero.homerunhome.game.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerozero.homerunhome.game.dto.GameResponse;
import org.zerozero.homerunhome.game.service.GameService;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
@Tag(name = "야구 경기", description = "저장된 야구 경기 목록 조회")
public class GameController {

    private final GameService gameService;

    @GetMapping
    @Operation(summary = "경기 목록 조회", description = "DB에 저장된 모든 경기를 날짜와 시간순으로 조회합니다.")
    public List<GameResponse> getGames() {
        return gameService.getGames();
    }
}
