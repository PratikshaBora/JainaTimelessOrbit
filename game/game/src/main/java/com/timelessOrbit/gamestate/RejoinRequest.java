package com.timelessOrbit.gamestate;

public class RejoinRequest {
	private int roomId;
    private String username;
    private String mobileNumber;

    // --- Getters & Setters ---
    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

}
