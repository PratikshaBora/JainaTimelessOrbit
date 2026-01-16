package com.timelessOrbit.gamestate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class PlayerScore {
	@Id
	private int playerId;
    private String username;
    private String mobileNumber;
    private int points;
    private int roomId;
    private long activeRoomTime;
    private int bonus;

    public PlayerScore() {
		super();
	}
	public PlayerScore(int playerId, String username, String mobileNumber, int points, int roomId, long activeRoomTime) {
        this.playerId = playerId;
        this.username = username;
        this.mobileNumber = mobileNumber;
        this.points = points;
        this.roomId = roomId;
        this.activeRoomTime = activeRoomTime;
    }
    // --- Getters ---
    public int getPlayerId() {
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
    public int getRoomId() {
        return roomId;
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
    public void setRoomId(int roomId) {
        this.roomId = roomId;
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
        return "PlayerScore [playerId=" + playerId +
               ", username=" + username +
               ", mobile number="+ mobileNumber +
               ", points=" + points +
               ", roomId=" + roomId + 
               ", active room time="+ activeRoomTime +"]";
    }
}