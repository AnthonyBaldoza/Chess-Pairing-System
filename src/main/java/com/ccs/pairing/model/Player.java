package com.ccs.pairing.model;

import jakarta.persistence.*;

@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    // link back to the master roster entry this player came from (null for
    // one-off manual entries typed before this feature existed / not yet synced)
    @ManyToOne
    @JoinColumn(name = "profile_id")
    private PlayerProfile profile;

    private String name;
    private int rating;
    private Integer age;
    private String gender; // "Male" / "Female"
    private double score = 0;
    private int wins = 0;
    private int losses = 0;
    private int draws = 0;
    private int gamesPlayed = 0;
    private int byeCount = 0;
    private int whitesPlayed = 0;
    private int blacksPlayed = 0;

    // comma-separated list of opponent player IDs already faced (keeps things simple, no join table)
    @Column(length = 2000)
    private String opponentIds = "";

    public Player() {}

    public Player(String name, int rating) {
        this.name = name;
        this.rating = rating;
    }

    public void addOpponent(Long opponentId) {
        if (opponentIds == null || opponentIds.isEmpty()) opponentIds = String.valueOf(opponentId);
        else opponentIds += "," + opponentId;
    }

    public boolean hasPlayed(Long opponentId) {
        if (opponentIds == null || opponentIds.isEmpty()) return false;
        for (String s : opponentIds.split(",")) {
            if (s.trim().equals(String.valueOf(opponentId))) return true;
        }
        return false;
    }

    // ── getters / setters ──
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Tournament getTournament() { return tournament; }
    public void setTournament(Tournament tournament) { this.tournament = tournament; }
    public PlayerProfile getProfile() { return profile; }
    public void setProfile(PlayerProfile profile) { this.profile = profile; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
    public int getDraws() { return draws; }
    public void setDraws(int draws) { this.draws = draws; }
    public int getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }
    public int getByeCount() { return byeCount; }
    public void setByeCount(int byeCount) { this.byeCount = byeCount; }
    public int getWhitesPlayed() { return whitesPlayed; }
    public void setWhitesPlayed(int whitesPlayed) { this.whitesPlayed = whitesPlayed; }
    public int getBlacksPlayed() { return blacksPlayed; }
    public void setBlacksPlayed(int blacksPlayed) { this.blacksPlayed = blacksPlayed; }
    public String getOpponentIds() { return opponentIds; }
    public void setOpponentIds(String opponentIds) { this.opponentIds = opponentIds; }
}
