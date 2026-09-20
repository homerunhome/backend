package org.zerozero.homerunhome.game.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.zerozero.homerunhome.game.domain.Game;

public interface GameRepository extends JpaRepository<Game, String> {

    List<Game> findAllByOrderByGameDateAscGameStartTimeAsc();
}
