package com.timelessOrbit.gamestate;

public class GameUpdate {
	private int roomId;
	private boolean success;
	private Player winner;
	
	// constructor + getters/setters
	public GameUpdate() {
		// TODO Auto-generated constructor stub
	}

	public GameUpdate(int roomId, boolean success, Player winner) {
		super();
		this.roomId = roomId;
		this.success = success;
		this.winner = winner;
	}

	public int getRoomId() {
		return roomId;
	}

	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public Player getWinner() {
		return winner;
	}

	public void setWinner(Player winner) {
		this.winner = winner;
	}
	
	
}
