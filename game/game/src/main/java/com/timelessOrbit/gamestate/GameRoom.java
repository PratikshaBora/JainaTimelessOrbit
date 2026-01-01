package com.timelessOrbit.gamestate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameRoom {

	private int id;
	public List<Player> players = new ArrayList<>();
	public List<Card> drawPile = new ArrayList<>();
	public List<Card> discardPile = new ArrayList<>();
	private String currentAara;
	public int currentPlayerIndex = 0;
	public boolean clockwise = true;

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getCurrentAara() {
		return currentAara;
	}
	public void setCurrentAara(String currentAara) {
		this.currentAara = currentAara;
	}
	public void prepare_card() {
		for (Aara a : Aara.values()) {
			for (Dwar d : Dwar.values()) {
				if (d == Dwar.COLOR_CHANGE && (a == Aara.FIRST || a == Aara.THIRD || a == Aara.FIFTH))
					continue;
				if (d == Dwar.COLOR_CHANGE_ADD4 && (a == Aara.SECOND || a == Aara.FOURTH || a == Aara.SIXTH))
					continue;

				drawPile.add(new Card(a, d));
			}
		}
	}
	public void distribute() {
		Collections.shuffle(drawPile);

		// Deal 5 cards one by one in rotation
		for (int round = 0; round < 5; round++) {
			for (Player p : players) {
				p.getHand().add(drawPile.remove(0));
			}
		}
		discardPile.add(drawPile.remove(0));
	}
	public void addPlayer(Player p) {
		if (players.size() < 4) {
			players.add(p);
		}
	}
	public String removePlayer(int pid) {
	    for (Player player : new ArrayList<>(players)) { // avoid ConcurrentModification
	        if (player.getId() == pid) {
	            players.remove(player); // ✅ remove by object, not index
	            return player.getUsername() + " left the room.";
	        }
	    }
	    return null;
	}
	public Player getCurrentPlayer() {
		return players.get(currentPlayerIndex);
	}
	public void nextTurn() {
		if (clockwise) {
			currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
		} else {
			currentPlayerIndex = (currentPlayerIndex - 1 + players.size()) % players.size();
		}
	}
	public boolean isDrawPileEmpty() {
		return drawPile.isEmpty();
	}
	public int calculatePoints(Player p) {
		int sum = 0;
		for (Card c : p.getHand()) {
			sum += c.getPointValue();
		}
		return sum;
	}
	public Player getWinner() {
		Player winner = null;
		int lowestPoints = Integer.MAX_VALUE;

		for (Player p : players) {
			int points = calculatePoints(p);
			if (points < lowestPoints) {
				lowestPoints = points;
				winner = p;
			}
		}
		return winner;
	}
	public boolean isGameOver() {
		return drawPile.isEmpty();
	}
	public boolean playCard(Player player, Card card) {
		GameEngine engine = new GameEngine(this);
		// Only allow if it's the player's turn
		if (player != getCurrentPlayer()) {
			return false;
		}
		engine.playTurn(player, card);
		return true;
	}
	public List<Player> getPlayers() {
		// TODO Auto-generated method stub
		return players;
	}
	// Return the top card of discard pile
	public Card getTopDiscard() {
		if (discardPile == null || discardPile.isEmpty()) {
			return null;
		}
		return discardPile.get(discardPile.size() - 1);
	}
	public Player getNextPlayer(int currentPlayerId) {
	    int index = -1;
	    for (int i = 0; i < players.size(); i++) {
	        if (players.get(i).getId() == currentPlayerId) {
	            index = i;
	            break;
	        }
	    }
	    if (index == -1) return null;

	    int nextIndex;
	    if (clockwise) {
	        nextIndex = (index + 1) % players.size();
	    } else {
	        nextIndex = (index - 1 + players.size()) % players.size();
	    }
	    return players.get(nextIndex);
	}
	public GameRoomDTO toDTO() {
	    GameRoomDTO dto = new GameRoomDTO();
	    dto.setRoomId(this.id);
	    // Convert each Player to PlayerDTO
	    List<PlayerDTO> playerDTOs = new ArrayList<>();
	    for (Player p : this.players) {
	        PlayerDTO pdto = new PlayerDTO();
	        pdto.setId(p.getId());
	        pdto.setUsername(p.getUsername());
	        pdto.setPoints(p.getPoints());
	        pdto.setHand(new ArrayList<>(p.getHand())); // shallow copy of hand
	        playerDTOs.add(pdto);
	    }
	    dto.setPlayers(playerDTOs);
	    dto.setDrawPile(new ArrayList<>(this.drawPile));
	    dto.setDiscardPile(new ArrayList<>(this.discardPile));
	    dto.setCurrentPlayerId(this.players.get(currentPlayerIndex).getId());
	    dto.setClockwise(this.clockwise);
	    dto.setCurrentAara(this.currentAara);
	    return dto;
	}
}
