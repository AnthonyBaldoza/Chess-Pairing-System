package com.ccs.pairing.model;

import jakarta.persistence.*;

// Master roster entry: one row per real-world player, kept across tournaments.
// Whenever a Player's rating changes inside a tournament (see TournamentService),
// the linked PlayerProfile.rating is synced so the roster always shows each
// player's most up-to-date rating without any manual re-entry.
@Entity
@Table(name = "player_profiles")
public class PlayerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    private int rating;
    private Integer age;
    private String gender; // "Male" / "Female"

    // total tournaments this player has been entered into (just informational)
    private int tournamentsPlayed = 0;

    public PlayerProfile() {}

    public PlayerProfile(String name, int rating) {
        this.name = name;
        this.rating = rating;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public int getTournamentsPlayed() { return tournamentsPlayed; }
    public void setTournamentsPlayed(int tournamentsPlayed) { this.tournamentsPlayed = tournamentsPlayed; }
}
