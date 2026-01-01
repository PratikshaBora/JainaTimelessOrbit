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
            if (!room.drawPile.isEmpty())
            	drawn.add(room.drawPile.remove(0));
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
                p.getHand().addAll(draw(2));
                nextPlayer();
                break;
            case COLOR_CHANGE:

            	break;
            case COLOR_CHANGE_ADD4:
                Player p2 = nextPlayer();
                p2.getHand().addAll(draw(4));
                nextPlayer();
                break;
		default:
			break;
        }
    }
    public void playTurn(Player player, Card chosen) {
        Card top = room.discardPile.get(room.discardPile.size() - 1);
        if (isValidMove(chosen, top)) {
            player.getHand().remove(chosen);
            room.discardPile.add(chosen);
            applyAction(chosen);
        } else {
            Card drawn = draw(1).get(0);
            player.getHand().add(drawn);

            if (isValidMove(drawn, top)) {
                player.getHand().remove(drawn);
                room.discardPile.add(drawn);
                applyAction(drawn);
            }
        }
        if (player.getHand().size() == 1 && !player.saidJaiJinendra) {
            player.getHand().add(draw(1).get(0));
        }
        if (player.getHand().isEmpty()) {
            System.out.println(player.username + " wins!");
        }
        nextPlayer();
    }
    public boolean skipTurn() {
        // Simply advance to the next player without letting current play
        nextPlayer();
        return true;
    }
    // methods for controller
	public boolean skipTurn(int playerId) {
	    Player current = currentPlayer();
	    if (current.getId() == playerId) {
	        nextPlayer();
	        return true;
	    }
	    return false;
	}
	public boolean reverseTurn() {
	    room.clockwise = !room.clockwise;
	    return true;
	}
	public void applyColorChange(String aara) {
	    room.setCurrentAara(aara);
	}
	public void drawToPlayer(Player player, int count) {
	    List<Card> drawn = draw(count);
	    player.getHand().addAll(drawn);
	}
}
