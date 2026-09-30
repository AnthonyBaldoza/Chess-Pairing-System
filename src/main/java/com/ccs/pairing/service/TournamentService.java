package com.ccs.pairing.service;

import com.ccs.pairing.model.Match;
import com.ccs.pairing.model.Player;
import com.ccs.pairing.model.PlayerProfile;
import com.ccs.pairing.model.Tournament;
import com.ccs.pairing.repository.MatchRepository;
import com.ccs.pairing.repository.PlayerProfileRepository;
import com.ccs.pairing.repository.PlayerRepository;
import com.ccs.pairing.repository.TournamentRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TournamentService {

    private final TournamentRepository tournamentRepo;
    private final PlayerRepository playerRepo;
    private final MatchRepository matchRepo;
    private final PlayerProfileRepository profileRepo;

    // simple Elo constants (mirrors the original console app)
    private static final int K_NEW = 40, K_MID = 20, K_HIGH = 10;
    private static final int HIGH_RATING = 2400, NEW_PLAYER_GAMES = 30;

    public TournamentService(TournamentRepository tournamentRepo, PlayerRepository playerRepo,
                              MatchRepository matchRepo, PlayerProfileRepository profileRepo) {
        this.tournamentRepo = tournamentRepo;
        this.playerRepo = playerRepo;
        this.matchRepo = matchRepo;
        this.profileRepo = profileRepo;
    }

    // ── Tournament / player setup ──────────────────────────────────────────
    public Tournament createTournament(String name, int rounds) {
        return tournamentRepo.save(new Tournament(name, rounds));
    }

    // Manual "type a name" add. Also upserts the master roster (player_profiles)
    // so this player becomes selectable in future tournaments without retyping.
    public Player addPlayer(Long tournamentId, String name, int rating, Integer age, String gender) {
        Tournament t = tournamentRepo.findById(tournamentId).orElseThrow();
        Player p = new Player(name, rating);
        p.setTournament(t);
        p.setAge(age);
        p.setGender(gender);

        PlayerProfile profile = profileRepo.findByNameIgnoreCase(name.trim()).orElse(null);
        if (profile == null) {
            profile = new PlayerProfile(name.trim(), rating);
        } else {
            profile.setRating(rating); // explicit manual entry wins over stored value
        }
        if (age != null) profile.setAge(age);
        if (gender != null) profile.setGender(gender);
        profile.setTournamentsPlayed(profile.getTournamentsPlayed() + 1);
        profile = profileRepo.save(profile);
        p.setProfile(profile);

        return playerRepo.save(p);
    }

    // ── Master roster (players saved from any past tournament) ────────────
    public List<PlayerProfile> allPlayerProfiles() {
        return profileRepo.findAllByOrderByNameAsc();
    }

    public List<PlayerProfile> searchPlayerProfiles(String query) {
        if (query == null || query.isBlank()) return allPlayerProfiles();
        return profileRepo.findByNameContainingIgnoreCaseOrderByNameAsc(query.trim());
    }

    // Add one or more previously-registered players into a tournament in one
    // shot, each using their current (auto-updated) roster rating — no manual
    // retyping and no re-checking ratings by hand every tournament.
    public List<Player> addPlayersFromProfiles(Long tournamentId, List<Long> profileIds) {
        Tournament t = tournamentRepo.findById(tournamentId).orElseThrow();
        List<Player> added = new ArrayList<>();
        for (Long profileId : profileIds) {
            PlayerProfile profile = profileRepo.findById(profileId).orElseThrow();
            Player p = new Player(profile.getName(), profile.getRating());
            p.setTournament(t);
            p.setProfile(profile);
            p.setAge(profile.getAge());
            p.setGender(profile.getGender());
            added.add(playerRepo.save(p));
            profile.setTournamentsPlayed(profile.getTournamentsPlayed() + 1);
            profileRepo.save(profile);
        }
        return added;
    }

    // Keep the master roster's rating in sync with a player's latest computed
    // rating so the next tournament always offers the up-to-date number.
    private void syncProfileRating(Player p) {
        if (p.getProfile() == null) return;
        PlayerProfile profile = p.getProfile();
        profile.setRating(p.getRating());
        profileRepo.save(profile);
    }

    // Fix a typo'd name or a wrong rating after the fact. Also keeps the
    // master roster in sync so the correction carries over to future tournaments.
    public Player updatePlayer(Long playerId, String name, Integer rating, Integer age, String gender) {
        Player p = playerRepo.findById(playerId).orElseThrow();
        if (name != null && !name.isBlank()) p.setName(name.trim());
        if (rating != null) p.setRating(rating);
        if (age != null) p.setAge(age);
        if (gender != null) p.setGender(gender);
        p = playerRepo.save(p);

        if (p.getProfile() != null) {
            PlayerProfile profile = p.getProfile();
            if (name != null && !name.isBlank()) profile.setName(name.trim());
            if (rating != null) profile.setRating(rating);
            if (age != null) profile.setAge(age);
            if (gender != null) profile.setGender(gender);
            profileRepo.save(profile);
        }
        return p;
    }

    // ── Delete / reset ──────────────────────────────────────────────────
    // Remove a single player from a tournament (e.g. added by mistake / test
    // data). Their matches in that tournament are removed too so nothing is
    // left dangling. The master roster entry is untouched.
    public void deletePlayerFromTournament(Long playerId) {
        Player p = playerRepo.findById(playerId).orElseThrow();
        List<Match> theirMatches = matchRepo.findByWhiteIdOrBlackId(playerId, playerId);
        matchRepo.deleteAll(theirMatches);
        playerRepo.delete(p);
    }

    // Wipe an entire tournament (all its players + matches) for a clean
    // restart, e.g. after testing. The master roster is untouched.
    public void deleteTournament(Long tournamentId) {
        List<Match> matches = matchRepo.findByTournamentIdOrderByRoundAscBoardNumberAsc(tournamentId);
        matchRepo.deleteAll(matches);
        List<Player> players = playerRepo.findByTournamentIdOrderByScoreDescRatingDesc(tournamentId);
        playerRepo.deleteAll(players);
        tournamentRepo.deleteById(tournamentId);
    }

    // Remove one saved player from the master roster (does not touch any
    // tournament they've already played in).
    public void deleteRosterProfile(Long profileId) {
        profileRepo.deleteById(profileId);
    }

    // Wipe the entire master roster, e.g. to start fresh with brand-new
    // ratings instead of carrying over old test data.
    public void clearRoster() {
        profileRepo.deleteAll();
    }

    // ── End-of-tournament awards: overall top 3 plus category winners ─────
    private static final int KIDDIE_MAX_AGE = 12;
    private static final int JUNIOR_MAX_AGE = 17;

    public AwardsResult awards(Long tournamentId) {
        List<Player> sorted = standings(tournamentId); // already sorted by score desc, rating desc

        List<Player> top3 = sorted.stream().limit(3).toList();

        Player topKiddie = sorted.stream()
            .filter(p -> p.getAge() != null && p.getAge() <= KIDDIE_MAX_AGE)
            .findFirst().orElse(null);

        Player topJunior = sorted.stream()
            .filter(p -> p.getAge() != null && p.getAge() > KIDDIE_MAX_AGE && p.getAge() <= JUNIOR_MAX_AGE)
            .findFirst().orElse(null);

        Player topLady = sorted.stream()
            .filter(p -> "Female".equalsIgnoreCase(p.getGender()))
            .findFirst().orElse(null);

        return new AwardsResult(top3, topKiddie, topJunior, topLady);
    }

    public List<Player> standings(Long tournamentId) {
        return playerRepo.findByTournamentIdOrderByScoreDescRatingDesc(tournamentId);
    }

    public List<Match> allMatches(Long tournamentId) {
        return matchRepo.findByTournamentIdOrderByRoundAscBoardNumberAsc(tournamentId);
    }

    // ── Suggested FIDE round count, same table as the console version ─────
    public static int suggestedRounds(int playerCount) {
        if (playerCount <= 8) return 5;
        if (playerCount <= 16) return 6;
        if (playerCount <= 32) return 7;
        if (playerCount <= 64) return 8;
        if (playerCount <= 128) return 9;
        if (playerCount <= 256) return 10;
        return 11;
    }

    // ── Pairing: sort by score/rating, pair down the list avoiding repeat
    //    opponents where possible (single greedy pass, no backtracking —
    //    good enough for a small club tournament). ─────────────────────────
    public List<Match> generatePairings(Long tournamentId) {
        Tournament t = tournamentRepo.findById(tournamentId).orElseThrow();
        if (t.getCurrentRound() >= t.getTotalRounds()) {
            throw new IllegalStateException(
                "This tournament is already at its final round (" + t.getTotalRounds() +
                "). No more rounds can be generated. Edit the total rounds if you need more.");
        }
        List<Player> sorted = new ArrayList<>(playerRepo.findByTournamentIdOrderByScoreDescRatingDesc(tournamentId));

        int nextRound = t.getCurrentRound() + 1;
        List<Match> matches = new ArrayList<>();

        // odd number of players -> bye for lowest-ranked player without one yet
        Player byePlayer = null;
        if (sorted.size() % 2 != 0) {
            for (int i = sorted.size() - 1; i >= 0; i--) {
                if (sorted.get(i).getByeCount() == 0) {
                    byePlayer = sorted.remove(i);
                    break;
                }
            }
            if (byePlayer == null) byePlayer = sorted.remove(sorted.size() - 1);
            byePlayer.setScore(byePlayer.getScore() + 1);
            byePlayer.setByeCount(byePlayer.getByeCount() + 1);
            playerRepo.save(byePlayer);

            Match byeMatch = new Match(t, nextRound, 0, byePlayer, null);
            byeMatch.setBye(true);
            byeMatch.setResultWhite(null);
            matches.add(matchRepo.save(byeMatch));
        }

        boolean[] used = new boolean[sorted.size()];
        int board = 1;
        for (int i = 0; i < sorted.size(); i++) {
            if (used[i]) continue;
            Player p1 = sorted.get(i);
            int opponentIdx = -1;
            for (int j = i + 1; j < sorted.size(); j++) {
                if (used[j]) continue;
                if (!p1.hasPlayed(sorted.get(j).getId())) { opponentIdx = j; break; }
            }
            // fall back to next available player even if it's a rematch
            if (opponentIdx == -1) {
                for (int j = i + 1; j < sorted.size(); j++) {
                    if (!used[j]) { opponentIdx = j; break; }
                }
            }
            if (opponentIdx == -1) continue; // odd leftover already handled above

            Player p2 = sorted.get(opponentIdx);
            used[i] = true;
            used[opponentIdx] = true;

            Player white = p1.getWhitesPlayed() <= p2.getWhitesPlayed() ? p1 : p2;
            Player black = (white == p1) ? p2 : p1;

            Match m = new Match(t, nextRound, board++, white, black);
            matches.add(matchRepo.save(m));
        }

        t.setCurrentRound(nextRound);
        t.setStatus(nextRound >= t.getTotalRounds() ? Tournament.Status.COMPLETE : Tournament.Status.IN_PROGRESS);
        tournamentRepo.save(t);

        return matches;
    }

    // ── Submit one board's result: 1 = white win, 0.5 = draw, 0 = black win
    public void submitResult(Long matchId, double resultWhite) {
        Match m = matchRepo.findById(matchId).orElseThrow();
        if (m.isBye()) return; // byes are settled at pairing time
        if (m.getResultWhite() != null && m.getResultWhite() >= 0) {
            // already scored once — undo previous stats before re-applying
            undoMatchStats(m);
        }

        Player white = m.getWhite();
        Player black = m.getBlack();
        double s1 = resultWhite;
        double s2 = 1 - resultWhite;

        applyResult(white, s1);
        applyResult(black, s2);

        white.setWhitesPlayed(white.getWhitesPlayed() + 1);
        black.setBlacksPlayed(black.getBlacksPlayed() + 1);
        white.addOpponent(black.getId());
        black.addOpponent(white.getId());

        int newWhiteRating = updateRating(white.getRating(), black.getRating(), s1, white.getGamesPlayed());
        int newBlackRating = updateRating(black.getRating(), white.getRating(), s2, black.getGamesPlayed());
        white.setRating(newWhiteRating);
        black.setRating(newBlackRating);

        playerRepo.save(white);
        playerRepo.save(black);
        syncProfileRating(white);
        syncProfileRating(black);

        m.setResultWhite(resultWhite);
        matchRepo.save(m);
    }

    private void applyResult(Player p, double s) {
        p.setScore(p.getScore() + s);
        p.setGamesPlayed(p.getGamesPlayed() + 1);
        if (s == 1) p.setWins(p.getWins() + 1);
        else if (s == 0.5) p.setDraws(p.getDraws() + 1);
        else p.setLosses(p.getLosses() + 1);
    }

    private void undoMatchStats(Match m) {
        Player white = m.getWhite();
        Player black = m.getBlack();
        double oldWhite = m.getResultWhite();
        double oldBlack = 1 - oldWhite;
        white.setScore(white.getScore() - oldWhite);
        black.setScore(black.getScore() - oldBlack);
        white.setGamesPlayed(white.getGamesPlayed() - 1);
        black.setGamesPlayed(black.getGamesPlayed() - 1);
        if (oldWhite == 1) white.setWins(white.getWins() - 1);
        else if (oldWhite == 0.5) white.setDraws(white.getDraws() - 1);
        else white.setLosses(white.getLosses() - 1);
        if (oldBlack == 1) black.setWins(black.getWins() - 1);
        else if (oldBlack == 0.5) black.setDraws(black.getDraws() - 1);
        else black.setLosses(black.getLosses() - 1);
    }

    private static double expectedScore(int ra, int rb) {
        return 1.0 / (1 + Math.pow(10, (rb - ra) / 400.0));
    }

    private static int kFactor(int rating, int gamesPlayed) {
        if (gamesPlayed < NEW_PLAYER_GAMES) return K_NEW;
        if (rating >= HIGH_RATING) return K_HIGH;
        return K_MID;
    }

    private static int updateRating(int rating, int opponentRating, double score, int gamesPlayed) {
        double expected = expectedScore(rating, opponentRating);
        int k = kFactor(rating, gamesPlayed);
        int newRating = (int) Math.round(rating + k * (score - expected));
        return Math.max(100, newRating);
    }

    public List<Match> currentRoundMatches(Long tournamentId) {
        Tournament t = tournamentRepo.findById(tournamentId).orElseThrow();
        return matchRepo.findByTournamentIdAndRound(tournamentId, t.getCurrentRound());
    }

    public Tournament get(Long tournamentId) {
        return tournamentRepo.findById(tournamentId).orElseThrow();
    }

    public List<Tournament> allTournaments() {
        return tournamentRepo.findAll();
    }
}
