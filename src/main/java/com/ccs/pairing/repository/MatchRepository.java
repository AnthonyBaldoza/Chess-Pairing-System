package com.ccs.pairing.repository;

import com.ccs.pairing.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByTournamentIdAndRound(Long tournamentId, int round);
    List<Match> findByTournamentIdOrderByRoundAscBoardNumberAsc(Long tournamentId);
    List<Match> findByWhiteIdOrBlackId(Long whiteId, Long blackId);
}
