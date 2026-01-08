package com.timelessOrbit.gamestate;

import java.util.ArrayList;
import java.util.List;

public class PlayerDTO {
    private int id;
    private String username;
    private int points;
    private List<Card> hand;   // ✅ each player’s in-hand cards
    private int handCount;

    public PlayerDTO() {}

    public PlayerDTO(int id, String username, int points, List<Card> hand) {
        this.id = id;
        this.username = username;
        this.points = points;
        this.hand = hand != null ? new ArrayList<>(hand) : new ArrayList<>();
        this.handCount = this.hand.size();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public List<Card> getHand() { return hand; }
    public void setHand(List<Card> hand) {
        this.hand = hand != null ? new ArrayList<>(hand) : new ArrayList<>();
        this.handCount = this.hand.size();
    }

    public int getHandCount() { return handCount; }
    public void setHandCount(int handCount) { this.handCount = handCount; }

    // ✅ Convert DTO back to Player entity
    public Player toPlayer() {
        Player p = new Player();
        p.setId(this.id);
        p.setUsername(this.username);
        p.setPoints(this.points);
        p.setHand(this.hand != null ? new ArrayList<>(this.hand) : new ArrayList<>());
        // Optional: set other fields like roomId, mobileNumber, etc. if needed
        return p;
    }

    @Override
    public String toString() {
        return "PlayerDTO [id=" + id + ", username=" + username +
               ", points=" + points + ", hand=" + hand +
               ", handCount=" + handCount + "]";
    }
}