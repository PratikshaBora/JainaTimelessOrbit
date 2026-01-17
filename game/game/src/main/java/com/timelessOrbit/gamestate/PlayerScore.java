package com.timelessOrbit.gamestate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class PlayerScore {
	@Id
	private long playerId;
	private String username;
	private String mobileNumber;
	private int points;
	@ManyToOne
	@JoinColumn(name = "room_id") // ✅ single mapping
	private GameRoom room;

	private long activeRoomTime;
	private int bonus;

	public PlayerScore() {
		super();
	}

	public PlayerScore(int playerId, String username, String mobileNumber, int points, long activeRoomTime,
			GameRoom room) {
		this.playerId = playerId;
		this.username = username;
		this.mobileNumber = mobileNumber;
		this.points = points;
		this.activeRoomTime = activeRoomTime;
		this.room = room;
	}

	// --- Getters ---
	public long getPlayerId() {
		return playerId;
	}

	public String getUsername() {
		return username;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public int getPoints() {
		return points;
	}

	public GameRoom getRoom() {
		return room;
	}

	public void setRoomId(GameRoom room) {
		this.room = room;
	}

	public long getActiveRoomTime() {
		return activeRoomTime;
	}

	// --- Setters (optional, if you want mutability) ---
	public void setPlayerId(int playerId) {
		this.playerId = playerId;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setMobileNumber(String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public void setActiveRoomTime(long setActiveRoom) {
		this.activeRoomTime = setActiveRoom;
	}

	public int getBonus() {
		return bonus;
	}

	public void setBonus(int bonus) {
		this.bonus = bonus;
	}

	@Override
	public String toString() {
		return "PlayerScore [playerId=" + playerId + ", username=" + username + ", mobile number=" + mobileNumber
				+ ", points=" + points + ", roomId=" + room.getId() + ", active room time=" + activeRoomTime + "]";
	}
}