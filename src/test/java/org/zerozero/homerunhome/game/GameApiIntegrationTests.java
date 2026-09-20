package org.zerozero.homerunhome.game;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.zerozero.homerunhome.game.domain.Game;
import org.zerozero.homerunhome.game.repository.GameRepository;

@SpringBootTest
@ActiveProfiles("test")
class GameApiIntegrationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private GameRepository gameRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        gameRepository.deleteAll();
        gameRepository.save(Game.builder()
                .gameId("game-2026-09-20-001")
                .city("대전")
                .gameDate(LocalDate.of(2026, 9, 20))
                .gameStartTime(LocalTime.of(18, 30))
                .stadium("대전 한화생명 볼파크")
                .homeTeam("한화 이글스")
                .awayTeam("롯데 자이언츠")
                .build());
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void listsAllGamesInScheduleOrder() throws Exception {
        gameRepository.save(Game.builder()
                .gameId("game-2026-09-21-001")
                .city("대전")
                .gameDate(LocalDate.of(2026, 9, 21))
                .gameStartTime(LocalTime.of(18, 30))
                .stadium("대전 한화생명 볼파크")
                .homeTeam("한화 이글스")
                .awayTeam("두산 베어스")
                .build());
        mockMvc.perform(get("/api/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].gameId").value("game-2026-09-20-001"))
                .andExpect(jsonPath("$[0].stadium").value("대전 한화생명 볼파크"))
                .andExpect(jsonPath("$[1].gameId").value("game-2026-09-21-001"));
    }

    @Test
    void exposesGameListInOpenApiDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/games'].get").exists());
    }
}
