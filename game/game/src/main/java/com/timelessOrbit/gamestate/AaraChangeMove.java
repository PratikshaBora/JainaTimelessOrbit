package com.timelessOrbit.gamestate;

public class AaraChangeMove {
	private int roomId;
    private int playerId;
    private Aara aara;

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

    public Aara getAara() { 
    	return aara; 
    }
    public void setAara(Aara aara) { 
    	this.aara = aara; 
    }
}
