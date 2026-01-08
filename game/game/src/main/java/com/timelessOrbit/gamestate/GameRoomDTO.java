package com.timelessOrbit.gamestate;

import java.util.List;

public class GameRoomDTO {
	 private int roomId;
	 private List<PlayerDTO> players;
	 private List<Card> discardPile;
	 private List<Card> drawPile;
	 private int currentPlayerId;
	 private boolean clockwise=true;
	 private String currentAara; // chosen color (aara) if wild was played
	 private int turnTimeLeft; // seconds remaining for current player's turn

	 public int getTurnTimeLeft() {
	     return turnTimeLeft;
	 }
	 public void setTurnTimeLeft(int turnTimeLeft) {
	     this.turnTimeLeft = turnTimeLeft;
	 }	 
	 public int getRoomId() {
		return roomId;
	 }
	 public void setRoomId(int roomId) {
		this.roomId = roomId;
	 }
	 public List<PlayerDTO> getPlayers() {
		return players;
	 }
	 public void setPlayers(List<PlayerDTO> players) {
		this.players = players;
	 }
	 public List<Card> getDiscardPile() {
		return discardPile;
	 }
	 public void setDiscardPile(List<Card> discardPile) {
		this.discardPile = discardPile;
	 }
	 public List<Card> getDrawPile() {
		return drawPile;
	 }
	 public void setDrawPile(List<Card> drawPile) {
		this.drawPile = drawPile;
	 }	 
	 public int getCurrentPlayerId() { 
		 return currentPlayerId; 
	 }
	 public void setCurrentPlayerId(int currentPlayerId) { 
		 this.currentPlayerId = currentPlayerId; 
	 }
	 public boolean isClockwise() { 
		 return clockwise; 
	 }
	 public void setClockwise(boolean clockwise) { 
		 this.clockwise = clockwise; 
	 }
	 public String getCurrentAara() { 
		 return currentAara; 
	 }
	 public void setCurrentAara(String currentAara) { 
		 this.currentAara = currentAara; 
	 }
	 @Override
    public String toString() {
        return "GameRoomDTO [roomId=" + roomId + ", players=" + players + ", discardPile=" + discardPile
                + ", drawPile=" + drawPile + ", currentPlayerId=" + currentPlayerId + ", clockwise=" + clockwise
                + ", currentAara=" + currentAara + ", turnTimeLeft=" + turnTimeLeft + "]";
    }
}
