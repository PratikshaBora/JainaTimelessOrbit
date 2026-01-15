package com.timelessOrbit.gamestate;

import java.util.ArrayList;
import java.util.List;

//	All game play logic here
public class GameEngine {
	GameRoom room;

	public GameEngine(GameRoom room) {
		this.room = room;
	}

	public Player currentPlayer() {
		System.out.println("current player: "+room.players.get(room.currentPlayerIndex));
		return room.players.get(room.currentPlayerIndex);
	}

	public Player nextPlayer() {
		int size = room.players.size();
		if (room.clockwise)
			room.currentPlayerIndex = (room.currentPlayerIndex + 1) % size;
		else
			room.currentPlayerIndex = (room.currentPlayerIndex - 1 + size) % size;

		return currentPlayer();
	}

	public void playTurn(Player player, Card chosen) {
		Card top = room.discardPile.get(room.discardPile.size() - 1);
		System.out.println("top card : " + top);
		room.setCurrentAara(top.getAara());
		
		if (isValidMove(chosen, top)) {
			player.getHand().remove(chosen);
			player.getHandCount();
			System.out.println("Player ("+player.getUsername()+") : "+player.getHandCount());
			room.discardPile.add(chosen);
			applyAction(chosen);
			nextPlayer();
		} 
		if (player.getHand().size() == 1 && !player.isSaidJaiJinendra()) {
			player.getHand().add(room.drawPile.get(0));
			nextPlayer();
		}
		if (player.getHand().isEmpty()) {
			System.out.println(player.getUsername() + " wins!");
		}
	}

	public boolean isValidMove(Card played, Card top) {
		// If a wild set a current aara, allow matching it
		if (room.getCurrentAara() != null) {
			if (played.getAara().equals(room.getCurrentAara())) {
				return true;
			}
		}
		// Always allow matching dwar
		if (played.getDwar() == top.getDwar()) {
			return true;
		}
		// Normal aara match
		if (played.getAara().equals(top.getAara())) {
			return true;
		}
		// Wild cards are always valid
		if (played.getType() == CardType.WILD) {
			return true;
		}
		return false;
	}

	public List<Card> draw(int count) {
		List<Card> drawn = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			if (!room.drawPile.isEmpty()) {
				drawn.add(room.drawPile.remove(0));
			}
			else {
				System.out.println("⚠️ Draw pile empty, cannot draw. Endgame should be resolved.");
			}
		}
		
		return drawn;
	}

	public void applyAction(Card card) {
		switch (card.dwar) {
		case SKIP:
			nextPlayer();
			break;
		case REVERSE:
			room.clockwise = !room.clockwise;
			break;
		case ADD2:
			Player p = nextPlayer();
			p.hand.addAll(draw(2));
			System.out.println("Card : "+card.dwar +", after apply handcount : "+p.getHandCount());
			break;
		case COLOR_CHANGE:
			room.setCurrentAara(card.newAara);
			break;
		case COLOR_CHANGE_ADD4:
			room.setCurrentAara(card.newAara);
			Player p2 = nextPlayer();
			p2.getHand().addAll(draw(4));
			break;
		default:
			break;
		}
	}

	public void applyAaraChange(Aara aara) {
		room.setCurrentAara(aara);
	}

	public void drawToPlayer(Player player, int count) {
		List<Card> drawn = draw(count);
		player.getHand().addAll(drawn);
	}
}
