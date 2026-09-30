package com.ccs.pairing.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tournaments")
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int totalRounds;
    private int currentRound = 0;

    @Enumerated(EnumType.STRING)
    private Status status = Status.SETUP;

    public enum Status { SETUP, IN_PROGRESS, COMPLETE }

    public Tournament() {}

    public Tournament(String name, int totalRounds) {
        this.name = name;
        this.totalRounds = totalRounds;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getTotalRounds() { return totalRounds; }
    public void setTotalRounds(int totalRounds) { this.totalRounds = totalRounds; }
    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int currentRound) { this.currentRound = currentRound; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
