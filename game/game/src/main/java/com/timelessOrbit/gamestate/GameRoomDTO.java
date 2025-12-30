package com.timelessOrbit.gamestate;

import java.util.List;

public class GameRoomDTO {
	 private int roomId;
	 private List<PlayerDTO> players;
	 private List<Card> discardPile;
	 private List<Card> drawPile;
	 
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
}
