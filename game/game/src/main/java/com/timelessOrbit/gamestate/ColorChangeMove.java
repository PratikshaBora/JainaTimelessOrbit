package com.timelessOrbit.gamestate;

public class ColorChangeMove {
	private int roomId;
    private int playerId;
    private String color;

    // getters and setters
    public int getRoomId() { 
    	return roomId; 
    }
    public void setRoomId(int roomId) { 
    	this.roomId = roomId; 
    }

    public int getPlayerId() { 
    	return playerId; 
    }
    public void setPlayerId(int playerId) { 
    	this.playerId = playerId; 
    }

    public String getColor() { 
    	return color; 
    }
    public void setColor(String color) { 
    	this.color = color; 
    }
}
