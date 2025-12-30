package com.timelessOrbit.gamestate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameRoom {

	private int id;
	public List<Player> players = new ArrayList<>();
	public List<Card> drawPile = new ArrayList<>();
	public List<Card> discardPile = new ArrayList<>();

	public int currentPlayerIndex = 0;
	public boolean clockwise = true;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
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

}
