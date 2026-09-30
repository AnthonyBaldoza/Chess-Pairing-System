package com.ccs.pairing.controller;

import com.ccs.pairing.model.Match;
import com.ccs.pairing.model.Player;
import com.ccs.pairing.model.PlayerProfile;
import com.ccs.pairing.model.Tournament;
import com.ccs.pairing.service.AwardsResult;
import com.ccs.pairing.service.TournamentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class TournamentController {

    private final TournamentService service;

    public TournamentController(TournamentService service) {
        this.service = service;
    }

    // ── Tournaments ──
    @GetMapping("/tournaments")
    public List<Tournament> listTournaments() {
        return service.allTournaments();
    }

    @PostMapping("/tournaments")
    public Tournament createTournament(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        int rounds = ((Number) body.getOrDefault("rounds", 5)).intValue();
        return service.createTournament(name, rounds);
    }

    @GetMapping("/tournaments/{id}")
    public Tournament getTournament(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/tournaments/suggest-rounds")
    public Map<String, Integer> suggestRounds(@RequestParam int playerCount) {
        return Map.of("suggestedRounds", TournamentService.suggestedRounds(playerCount));
    }

    // ── Players ──
    @PostMapping("/tournaments/{id}/players")
    public Player addPlayer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        int rating = ((Number) body.getOrDefault("rating", 1200)).intValue();
        Integer age = body.get("age") != null ? ((Number) body.get("age")).intValue() : null;
        String gender = (String) body.get("gender");
        return service.addPlayer(id, name, rating, age, gender);
    }

    @GetMapping("/tournaments/{id}/standings")
    public List<Player> standings(@PathVariable Long id) {
        return service.standings(id);
    }

    // ── Master player roster (reusable across tournaments) ──
    @GetMapping("/players")
    public List<PlayerProfile> allPlayers(@RequestParam(required = false) String q) {
        return (q == null || q.isBlank()) ? service.allPlayerProfiles() : service.searchPlayerProfiles(q);
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/tournaments/{id}/players/from-roster")
    public List<Player> addPlayersFromRoster(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        List<Object> raw = (List<Object>) body.get("profileIds");
        List<Long> profileIds = raw.stream().map(o -> ((Number) o).longValue()).toList();
        return service.addPlayersFromProfiles(id, profileIds);
    }

    // ── Pairings / matches ──
    @PostMapping("/tournaments/{id}/pairings")
    public List<Match> generatePairings(@PathVariable Long id) {
        return service.generatePairings(id);
    }

    @GetMapping("/tournaments/{id}/matches/current")
    public List<Match> currentRoundMatches(@PathVariable Long id) {
        return service.currentRoundMatches(id);
    }

    @GetMapping("/tournaments/{id}/matches")
    public List<Match> allMatches(@PathVariable Long id) {
        return service.allMatches(id);
    }

    @PostMapping("/matches/{matchId}/result")
    public void submitResult(@PathVariable Long matchId, @RequestBody Map<String, Object> body) {
        double result = ((Number) body.get("result")).doubleValue();
        service.submitResult(matchId, result);
    }

    // Fix a typo'd name or wrong rating for a player already added to a tournament.
    @PutMapping("/players/{playerId}")
    public Player updatePlayer(@PathVariable Long playerId, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Integer rating = body.get("rating") != null ? ((Number) body.get("rating")).intValue() : null;
        Integer age = body.get("age") != null ? ((Number) body.get("age")).intValue() : null;
        String gender = (String) body.get("gender");
        return service.updatePlayer(playerId, name, rating, age, gender);
    }

    // ── Delete / reset ──
    @DeleteMapping("/players/{playerId}")
    public void deletePlayerFromTournament(@PathVariable Long playerId) {
        service.deletePlayerFromTournament(playerId);
    }

    @DeleteMapping("/tournaments/{id}")
    public void deleteTournament(@PathVariable Long id) {
        service.deleteTournament(id);
    }

    @DeleteMapping("/players/roster/{profileId}")
    public void deleteRosterProfile(@PathVariable Long profileId) {
        service.deleteRosterProfile(profileId);
    }

    @DeleteMapping("/players/roster")
    public void clearRoster() {
        service.clearRoster();
    }

    // ── Awards: overall top 3 + Kiddie / Junior / Lady category winners ──
    @GetMapping("/tournaments/{id}/awards")
    public AwardsResult awards(@PathVariable Long id) {
        return service.awards(id);
    }

    // Friendly error messages (e.g. "already at final round") instead of a raw 500.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
