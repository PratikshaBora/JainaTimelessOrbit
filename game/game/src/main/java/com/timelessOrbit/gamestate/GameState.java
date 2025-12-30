package com.timelessOrbit.gamestate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component   // ✅ makes GameState managed by Spring
public class GameState {
	
	@Autowired
	SimpMessagingTemplate messagingTemplate;
	List<Player> waitingPlayers = new ArrayList<Player>();
	List<GameRoom> gameRooms = new ArrayList<GameRoom>();
	
	private List<PlayerScore> pastScores = new ArrayList<>();
	
	long firstPlayerJoinTime = 0;
	private final long timeoutMillis = 2 * 60 * 1000; // configurable later
	
	//When a player joins, they go into waitingPlayers.
	public void addPlayer(Player p) {
		System.out.println("-------GameState instance: " + this.hashCode()+"------------");
		waitingPlayers.add(p);
	    System.out.println(waitingPlayers);
	    System.out.println("No of waiting players : "+waitingPlayers.size());
	    
		// We need to track when the first player entered the queue.
	    if (waitingPlayers.size() == 1) {
		    firstPlayerJoinTime = System.currentTimeMillis();
		}
	}
	
	// ✅ run every 30- seconds
    @Scheduled(fixedRate = 30000)
    public void scheduledRoomCheck() {
//    	System.out.println("-------GameState instance: " + this.hashCode()+"------------");
    	System.out.println("inside scheduled"+waitingPlayers.size());
        checkRoom();   // call your existing method
    }

    public void checkRoom() {
//    	System.out.println("-------GameState instance: " + this.hashCode()+"------------");
//    	System.out.println("checking room to be created");
    	System.out.println("waiting players for room allotment: "+waitingPlayers.size());
        long now = System.currentTimeMillis();

        while (!waitingPlayers.isEmpty()) {
            int size = waitingPlayers.size();

            if (size >= 4) {
                createRoom(4);
            } else if (size > 1 && (now - firstPlayerJoinTime) >= timeoutMillis) {
                createRoom(size);
            } else if (size == 1 && (now - firstPlayerJoinTime) >= timeoutMillis) {
                messagingTemplate.convertAndSend("/topic/lobbyStatus",
                        "Only one player remaining, auto-adding player");
            } else {
                break; // wait for more players or timeout
            }
        }
    }

	
	public void createRoom(int count) {
	    GameRoom room = new GameRoom();
	    room.players = new ArrayList<>();
	    room.discardPile = new ArrayList<>();
	    
	    int roomId = gameRooms.size(); // simple auto-increment
	    room.setId(roomId);

	    for (int i = 0; i < count; i++) {
	    	if (!waitingPlayers.isEmpty()) {
	    	    Player p = waitingPlayers.remove(0);
	    	    p.setRoomId(roomId);		// assign room number
		        room.players.add(p);
	    	}
	    }
	    room.prepare_card();
	    room.distribute();
	    gameRooms.add(room);

	    System.out.println("✅ GameRoom " + roomId + " created with " + count + " players");
	    
//	    // NEW: broadcast room creation
//	    messagingTemplate.convertAndSend("/topic/rooms", room);
//	    // ✅ Broadcast game start
//	    messagingTemplate.convertAndSend("/topic/game", room);
	    
	    // 👉 Convert to DTO and broadcast here
	    GameRoomDTO dto = convertToDTO(room);
	    messagingTemplate.convertAndSend("/topic/game/" + roomId, dto);
	    
	    // Reset timer if queue still has players
	    if (!waitingPlayers.isEmpty()) {
	        firstPlayerJoinTime = System.currentTimeMillis();
	    }
	}	
	
	private GameRoomDTO convertToDTO(GameRoom room) {
		GameRoomDTO dto = new GameRoomDTO();
	    dto.setRoomId(room.getId());

	    List<PlayerDTO> playerDTOs = room.players.stream()
	        .map(p -> new PlayerDTO(p.getUsername(), p.getPoints(), p.getHand()))
	        .toList();

	    dto.setPlayers(playerDTOs);
	    dto.setDiscardPile(room.discardPile);
	    dto.setDrawPile(room.drawPile);

	    return dto;
	}

	// New overload: create a room from a specific list of players
	public GameRoom createRoom(List<Player> selectedPlayers) {
	    GameRoom room = new GameRoom();
	    room.players = new ArrayList<>();
	    room.discardPile = new ArrayList<>();

	    int roomId = gameRooms.size(); // auto-increment
	    room.setId(roomId);

	    for (Player p : selectedPlayers) {
	        // remove from waiting list if present
	        waitingPlayers.removeIf(wp -> wp.getId() == p.getId());
	        p.setRoomId(roomId);
	        room.getPlayers().add(p);
	    }

	    room.prepare_card();   // or rename to prepareCards()
	    room.distribute();     // or rename to distributeCards()

	    gameRooms.add(room);

	    System.out.println("✅ GameRoom " + roomId + " created with " + selectedPlayers.size() + " players");

	    if (!waitingPlayers.isEmpty()) {
	        firstPlayerJoinTime = System.currentTimeMillis();
	    }

	    return room;
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
	    gameRooms.remove(roomId); // mark as ended
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

	public Collection<? extends Player> getRoomPlayers() {
		// TODO Auto-generated method stub
		return null;
	}

}
