package com.timelessOrbit.gamestate;

import java.util.ArrayList;
import java.util.List;

public class Player {
	int id;
	String username;
	String password;
	int points;
	int roomId = -1; // -1 means not assigned to any room
	
	private List<Card> hand = new ArrayList<>();

	boolean saidJaiJinendra = false;
	
	public void changeSaidJaiJinendra(boolean val) {
		saidJaiJinendra = val;
	}
	
	public Player() {
	
	}
	
	public Player(String username, String password) {
		super();
		this.username = username;
		this.password = password;
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

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public int getRoomId() {
		return roomId;
	}

	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}

	public List<Card> getHand() {
		return hand;
	}

	public void setHand(List<Card> hand) {
		this.hand = hand;
	}

	// cards in hand is remaining in toString
	@Override
	public String toString() {
		return "Player [username=" + username + ", password=" + password + ", points=" + points + "]";
	}
}
