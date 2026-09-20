package org.zerozero.homerunhome.game.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerozero.homerunhome.game.dto.GameRegistrationRequest;
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

    @GetMapping("/{gameId}")
    @Operation(summary = "경기 상세 조회", description = "경기 식별자로 저장된 경기 정보를 조회합니다.")
    public GameResponse getGame(@PathVariable String gameId) {
        return gameService.getGame(gameId);
    }

    @PostMapping
    @Operation(summary = "경기 일정 등록", description = "경기 식별자와 경기 정보를 등록합니다. 이미 등록된 식별자는 거부합니다.")
    public ResponseEntity<GameResponse> register(@Valid @RequestBody GameRegistrationRequest request) {
        GameResponse response = gameService.register(request);
        return ResponseEntity.created(URI.create("/api/games/" + response.gameId())).body(response);
    }
}
