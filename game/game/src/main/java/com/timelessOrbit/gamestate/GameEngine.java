package com.timelessOrbit.gamestate;

import java.util.ArrayList;
import java.util.Collections;
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

        if (played.aara == top.aara) return true;
        if (played.dwar == top.dwar) return true;

        if (played.type == CardType.WILD) return true;

        return false;
    }
    
	/*
	 * public void reshuffle() { Card top =
	 * room.discardPile.remove(room.discardPile.size() - 1);
	 * Collections.shuffle(room.discardPile);
	 * room.drawPile.addAll(room.discardPile); room.discardPile.clear();
	 * room.discardPile.add(top); }
	 */

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
                // UI will set chosen āra
            	// need to select ara and should be applied to play
                break;

            case COLOR_CHANGE_ADD4:
                Player p2 = nextPlayer();
                p2.getHand().addAll(draw(4));
                nextPlayer();
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
}
