package org.keyyh.stickmanfighter.common.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "match_results")
public class MatchResult implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player1_id", nullable = false)
    private User player1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player2_id", nullable = false)
    private User player2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "match_date", insertable = false, updatable = false)
    private Date matchDate;

    // Constructors, Getters and Setters...
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public User getPlayer1() { return player1; }
    public void setPlayer1(User player1) { this.player1 = player1; }
    public User getPlayer2() { return player2; }
    public void setPlayer2(User player2) { this.player2 = player2; }
    public User getWinner() { return winner; }
    public void setWinner(User winner) { this.winner = winner; }
    public Date getMatchDate() { return matchDate; }
    public void setMatchDate(Date matchDate) { this.matchDate = matchDate; }

    @Override
    public String toString() {
        return "MatchResult{" + "id=" + id + ", player1=" + player1.getUsername() + ", player2=" + player2.getUsername() + ", winner=" + (winner != null ? winner.getUsername() : "Draw") + '}';
    }
}