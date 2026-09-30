package com.ccs.pairing.service;

import com.ccs.pairing.model.Player;

import java.util.List;

// Plain result object returned by TournamentService.awards() — overall top 3
// plus the best-placing player in each special category (kiddie/junior/lady).
// Any category with no eligible player is simply null.
public class AwardsResult {
    public List<Player> overallTop3;
    public Player topKiddie;   // age 12 and below
    public Player topJunior;   // age 13-17
    public Player topLady;     // best-scoring female player, any age

    public AwardsResult(List<Player> overallTop3, Player topKiddie, Player topJunior, Player topLady) {
        this.overallTop3 = overallTop3;
        this.topKiddie = topKiddie;
        this.topJunior = topJunior;
        this.topLady = topLady;
    }
}
