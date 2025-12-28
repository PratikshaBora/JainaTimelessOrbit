package com.timelessOrbit.gamestate;

import java.util.ArrayList;
import java.util.List;

public class GameState {
	
	List<Player> waitingPlayers = new ArrayList<Player>();
	List<GameRoom> gameRooms = new ArrayList<GameRoom>();
	
	private List<PlayerScore> pastScores = new ArrayList<>();
	
	long firstPlayerJoinTime = 0;
	
	//When a player joins, they go into waitingPlayers.
	public void addPlayer(Player p) {
	
	    waitingPlayers.add(p);
		// We need to track when the first player entered the queue.

		if (waitingPlayers.size() == 1) {
		    firstPlayerJoinTime = System.currentTimeMillis();
		}
		
	    tryCreateRoom();
	}
	
	/*
	 * This method checks: 
	 * - If 4 players are waiting, create a room immediately 
	 * - If 2 minutes passed, create a room with whoever is waiting (even 2 or 3
	 * players)
	 */
	private void tryCreateRoom() {
	    long now = System.currentTimeMillis();

	    // Case 1: 4 players ready
	    if (waitingPlayers.size() >= 4) {
	        createRoom(4);
	        return;
	    }

	    // Condition 2: 2 minutes passed
	    if (waitingPlayers.size() > 0 && (now - firstPlayerJoinTime) >= 2 * 60 * 1000) {
	        createRoom(waitingPlayers.size());
	    }
	}
	
	private void createRoom(int count) {
	    GameRoom room = new GameRoom();
	    room.players = new ArrayList<>();
	    room.discardPile = new ArrayList<>();
	    
	    int roomId = gameRooms.size(); // simple auto-increment

	    for (int i = 0; i < count; i++) {
	    	Player p = waitingPlayers.remove(0);
	    	p.setRoomId(roomId);		// assign room number
	        room.players.add(p);
	    }

	    room.prepare_card();
	    room.distribute();

	    gameRooms.add(room);

	    System.out.println("✅ GameRoom " + roomId + " created with " + count + " players");

	    // Reset timer if queue still has players
	    if (waitingPlayers.size() > 0) {
	        firstPlayerJoinTime = System.currentTimeMillis();
	    }
	}	
	
	public Player getCurrentPlayer(int roomId) {
	    return gameRooms.get(roomId).getCurrentPlayer();
	}
	
	public Player checkAndHandleGameOver(int roomId) {
	    GameRoom room = gameRooms.get(roomId);

	    // 1. Check if game is over
	    if (!room.isGameOver()) {
	        return null; // game still running
	    }

	    // 2. Determine winner
	    Player winner = room.getWinner();

	    // 3. Announce winner (server-side log)
	    System.out.println("🏁 Game Over in Room " + roomId + 
	                       ". Winner: " + winner.getUsername());

	    // 4. Cleanup room
	    endRoom(roomId);

	    // 5. Return winner to caller (UI, network, etc.)
	    return winner;
	}
	
	public List<PlayerScore> endRoom(int roomId) {
	    GameRoom room = gameRooms.get(roomId);
	    List<PlayerScore> scores = new ArrayList<>();

	    if (room != null) {
	        for (Player p : room.getPlayers()) {
	            int points = room.calculatePoints(p);
	            PlayerScore score = new PlayerScore(
	                p.getId(),
	                p.getUsername(),
	                points,
	                roomId
	            );
	            pastScores.add(score);
	            scores.add(score); // return snapshot for broadcast
	        }
	        System.out.println("Room " + roomId + " ended. Scores saved.");
	    }
	    gameRooms.set(roomId, null); // mark as ended
	    return scores;
	}


	// Access past scores
	public List<PlayerScore> getPastScores() {
	    return new ArrayList<>(pastScores);
	}

	
	public boolean playCard(int roomId, int playerId, Card card) {
	    GameRoom room = gameRooms.get(roomId);

	    // Find the player inside the room
	    Player player = null;
	    for (Player p : room.players) {
	        if (p.getId() == playerId) {
	            player = p;
	            break;
	        }
	    }
	    if (player == null) {
	        System.out.println("❌ Player not found in room");
	        return false;
	    }
	    // Delegate to GameRoom
	    boolean success = room.playCard(player, card);

	    // After playing, check if game ended
	    Player winner = checkAndHandleGameOver(roomId);
	    if (winner != null) {
	        System.out.println("🎉 Winner is: " + winner.getUsername());
	    }
	    return success;
	}

	public List<GameRoom> getGameRooms() {
		// TODO Auto-generated method stub
		return gameRooms;
	}
	
	// If player cannot play from hand, they must draw from draw pile.
	// If drawn card is playable, play immediately; else add to hand.
	public boolean tryPlayOrReturn(int roomId, Player player, Card chosenCard) {
	    GameRoom room = gameRooms.get(roomId);
	    GameEngine engine = new GameEngine(room);

	    // Step 1: if a card was explicitly chosen, try to play it
	    if (chosenCard != null) {
	        if (engine.isValidMove(chosenCard, room.getTopDiscard())) {
	            return room.playCard(player, chosenCard);
	        } else {
	            System.out.println("❌ Invalid move with chosen card");
	            return false;
	        }
	    }

	    // Step 2: check if player has any playable card in hand
	    for (Card c : player.getHand()) {
	        if (engine.isValidMove(c, room.getTopDiscard())) {
	            return true; // controller should call playCard() with that card
	        }
	    }

	    // Step 3: no playable card → draw from draw pile
	    if (!room.drawPile.isEmpty()) {
	        Card drawn = room.drawPile.remove(0);
	        if (engine.isValidMove(drawn, room.getTopDiscard())) {
	            return room.playCard(player, drawn);
	        } else {
	            player.getHand().add(drawn);
	            return false;
	        }
	    }

	    // Step 4: draw pile empty → endgame triggered externally
	    System.out.println("⚠️ Draw pile empty, cannot draw. Endgame should be resolved.");
	    return false;
	}
	// Endgame rule: lowest hand count wins, score = sum of others’ points
    public Player resolveEndgame(GameRoom room) {
        Player winner = null;
        int lowestHand = Integer.MAX_VALUE;

        for (Player p : room.players) {
            if (p.getHand().size() < lowestHand) {
                lowestHand = p.getHand().size();
                winner = p;
            }
        }

        int bonus = 0;
        for (Player p : room.players) {
            if (p != winner) {
                for (Card c : p.getHand()) {
                	bonus += c.getPointValue();
                }
            }
        }
        if (winner != null) {
//            winner.setScore(winner.getScore() + bonus);
        	 winner.setPoints(winner.getPoints() + bonus);  // ✅ use Player.setPoints()
        }

        System.out.println("🏁 Endgame triggered. Winner: " + winner.getUsername() +
                           " with bonus " + bonus);
        return winner;
    }

    // Convenience getter
    public GameRoom getRoom(int roomId) {
        return gameRooms.get(roomId);
    }

 // Remove player by ID (from lobby or from any room)
    public String removePlayer(int id) {
        // First check waitingPlayers (lobby)
        for (Player p : waitingPlayers) {
            if (p.getId() == id) {
                waitingPlayers.remove(p);
                return p.getUsername() + " left the lobby.";
            }
        }

        // Then check active rooms
        for (GameRoom room : gameRooms) {
            if (room != null) {
                String result = room.removePlayer(id);
                if (result != null) {
                    return result;
                }
            }
        }

        return "❌ Player with id " + id + " not found.";
    }


	// Return all players currently waiting in lobby
	public List<Player> getPlayers() {
	    return new ArrayList<>(waitingPlayers);
	}

}
