package org.zerozero.homerunhome.game.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "경기 일정 등록 요청")
public record GameRegistrationRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*") String gameId,
        @NotBlank @Size(max = 100) String city,
        @NotNull LocalDate gameDate,
        @NotNull LocalTime gameStartTime,
        @NotBlank @Size(max = 150) String stadium,
        @NotBlank @Size(max = 100) String homeTeam,
        @NotBlank @Size(max = 100) String awayTeam) {}
