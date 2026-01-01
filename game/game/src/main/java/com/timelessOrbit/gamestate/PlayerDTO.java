package com.timelessOrbit.gamestate;

import java.util.List;

public class PlayerDTO {
	int id;
	private String username;
    private int points;
    private List<Card> hand;  // ✅ each player’s in-hand cards
    
    
    public PlayerDTO() {
	}
	public PlayerDTO(String username, int points, List<Card> hand) {
		super();
		this.username = username;
		this.points = points;
		this.hand = hand;
	}
    public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public int getPoints() {
		return points;
	}
	public void setPoints(int points) {
		this.points = points;
	}
	public List<Card> getHand() {
		return hand;
	}
	public void setHand(List<Card> hand) {
		this.hand = hand;
	}
}
