package com.timelessOrbit.gamestate;

public class PlayerScore {
    private int playerId;
    private String username;
    private int points;
    private int roomId;

    public PlayerScore(int playerId, String username, int points, int roomId) {
        this.playerId = playerId;
        this.username = username;
        this.points = points;
        this.roomId = roomId;
    }

    // --- Getters ---
    public int getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    public int getPoints() {
        return points;
    }

    public int getRoomId() {
        return roomId;
    }

    // --- Setters (optional, if you want mutability) ---
    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    @Override
    public String toString() {
        return "PlayerScore [playerId=" + playerId +
               ", username=" + username +
               ", points=" + points +
               ", roomId=" + roomId + "]";
    }
}