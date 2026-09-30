package com.ccs.pairing.config;

import com.ccs.pairing.model.Player;
import com.ccs.pairing.model.PlayerProfile;
import com.ccs.pairing.repository.PlayerProfileRepository;
import com.ccs.pairing.repository.PlayerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;

// Runs once on every startup. If the master roster (player_profiles) is empty
// but there are already Player rows from earlier tournaments (created before
// the roster feature existed), rebuild the roster from them so those players
// are immediately reusable — no need to retype anyone.
@Component
public class RosterBackfillRunner implements CommandLineRunner {

    private final PlayerRepository playerRepo;
    private final PlayerProfileRepository profileRepo;

    public RosterBackfillRunner(PlayerRepository playerRepo, PlayerProfileRepository profileRepo) {
        this.playerRepo = playerRepo;
        this.profileRepo = profileRepo;
    }

    @Override
    public void run(String... args) {
        if (profileRepo.count() > 0) return; // already populated, nothing to do

        List<Player> all = playerRepo.findAll();
        if (all.isEmpty()) return;

        // group by name (case-insensitive), keep the entry with the highest id
        // (= most recently saved) so we grab each player's latest known rating
        Map<String, Player> latestByName = new LinkedHashMap<>();
        for (Player p : all) {
            if (p.getName() == null || p.getName().isBlank()) continue;
            String key = p.getName().trim().toLowerCase();
            Player existing = latestByName.get(key);
            if (existing == null || p.getId() > existing.getId()) {
                latestByName.put(key, p);
            }
        }

        for (Player p : latestByName.values()) {
            PlayerProfile profile = new PlayerProfile(p.getName().trim(), p.getRating());
            profile.setTournamentsPlayed(1);
            profileRepo.save(profile);
        }
    }
}
