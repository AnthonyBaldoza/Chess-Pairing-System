package com.ccs.pairing.model;

import jakarta.persistence.*;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    private int round;
    private int boardNumber;

    @ManyToOne
    @JoinColumn(name = "white_id")
    private Player white;

    @ManyToOne
    @JoinColumn(name = "black_id")
    private Player black;

    // null = bye (no black player), otherwise -1 = not played yet, 1 = white win, 0.5 = draw, 0 = black win
    private Double resultWhite = -1.0;

    private boolean bye = false;

    public Match() {}

    public Match(Tournament tournament, int round, int boardNumber, Player white, Player black) {
        this.tournament = tournament;
        this.round = round;
        this.boardNumber = boardNumber;
        this.white = white;
        this.black = black;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Tournament getTournament() { return tournament; }
    public void setTournament(Tournament tournament) { this.tournament = tournament; }
    public int getRound() { return round; }
    public void setRound(int round) { this.round = round; }
    public int getBoardNumber() { return boardNumber; }
    public void setBoardNumber(int boardNumber) { this.boardNumber = boardNumber; }
    public Player getWhite() { return white; }
    public void setWhite(Player white) { this.white = white; }
    public Player getBlack() { return black; }
    public void setBlack(Player black) { this.black = black; }
    public Double getResultWhite() { return resultWhite; }
    public void setResultWhite(Double resultWhite) { this.resultWhite = resultWhite; }
    public boolean isBye() { return bye; }
    public void setBye(boolean bye) { this.bye = bye; }
}
