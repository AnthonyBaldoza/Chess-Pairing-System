package com.ccs.pairing.repository;

import com.ccs.pairing.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByTournamentIdOrderByScoreDescRatingDesc(Long tournamentId);
}
