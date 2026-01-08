package com.timelessOrbit.gamestate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
	private int nextPlayerId = 1; // global counter
	
	private List<PlayerScore> pastScores = new ArrayList<>();
	
	long firstPlayerJoinTime = 0;
	private final long timeoutMillis = 2 * 60 * 1000; // configurable later
	
	// When a player joins, they go into waitingPlayers.
	public void addPlayer(Player p) {
	    System.out.println("player info received from frontend: " + p);

	    // ✅ Create backend Player with assigned ID
	    Player player = new Player();
	    player.setId(nextPlayerId++);
	    player.setUsername(p.getUsername());
	    player.setMobileNumber(p.getMobileNumber());
	    System.out.println("Populated player with id : " + player);

	    waitingPlayers.add(player);
	    System.out.println("No of waiting players : " + waitingPlayers.size());

	    // ✅ Convert to DTO
	    PlayerDTO dto = new PlayerDTO(
	        player.getId(),
	        player.getUsername(),
	        player.getPoints(),
	        new ArrayList<>(player.getHand()) // defensive copy
	    );

	    // ✅ Broadcast to frontend
	    messagingTemplate.convertAndSend("/topic/playerJoined", dto);

	    // ✅ Track when the first player entered the queue
	    if (waitingPlayers.size() == 1) {
	        firstPlayerJoinTime = System.currentTimeMillis();
	    }
	}	
	// ✅ run every 30- seconds
    @Scheduled(fixedRate = 30000)
    public void scheduledRoomCheck() {
//    	System.out.println("-------GameState instance: " + this.hashCode()+"------------");
    	System.out.println("inside scheduled : "+waitingPlayers.size());
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
        int roomId = gameRooms.size(); // simple auto-increment
        room.setId(roomId);

        // Assign players from waiting list
        for (int i = 0; i < count; i++) {
            if (!waitingPlayers.isEmpty()) {
                Player p = waitingPlayers.remove(0);
                p.setRoomId(roomId);          // assign room number
//                p.setId(nextPlayerId++);      // assign unique player ID
                room.players.add(p);
            }
        }

        // Prepare and distribute cards
        room.prepare_card();
        room.distribute();
        gameRooms.add(room);

        System.out.println("✅ GameRoom " + roomId + " created with " + count + " players");

        // --- Shared broadcast: announce room creation ---
        GameRoomDTO dto = convertToDTO(room);
        room.setGameDTO(dto);
        messagingTemplate.convertAndSend("/topic/rooms", dto);

        System.out.println(dto);

        // If players are still waiting, reset join time
        if (!waitingPlayers.isEmpty()) {
            firstPlayerJoinTime = System.currentTimeMillis();
        }
    }
	
    private GameRoomDTO convertToDTO(GameRoom room) {
        GameRoomDTO dto = new GameRoomDTO();
        dto.setRoomId(room.getId());

        // Convert each Player to PlayerDTO
        List<PlayerDTO> playerDTOs = room.players.stream()
            .map(p -> {
            	return new PlayerDTO(p.getId(), p.getUsername(), p.getPoints(), new ArrayList<>(p.getHand()));
            })
            .toList();

        dto.setPlayers(playerDTOs);
        dto.setDiscardPile(new ArrayList<>(room.discardPile));
        dto.setDrawPile(new ArrayList<>(room.drawPile));

        // ✅ Add turn info
        dto.setCurrentPlayerId(room.getCurrentPlayer().getId());
        dto.setClockwise(room.clockwise);
        dto.setCurrentAara(room.getCurrentAara());

        // ✅ Reset timer at start of each turn
        dto.setTurnTimeLeft(60);

        return dto;
    }

	public GameRoomDTO filterForPlayer(GameRoomDTO room, int requestingPlayerId) {
	    GameRoomDTO filtered = new GameRoomDTO();
	    filtered.setRoomId(room.getRoomId());
	    filtered.setDrawPile(room.getDrawPile());
	    filtered.setDiscardPile(room.getDiscardPile());
	    filtered.setCurrentPlayerId(room.getCurrentPlayerId());
	    filtered.setClockwise(room.isClockwise());
	    filtered.setCurrentAara(room.getCurrentAara());
	    filtered.setTurnTimeLeft(room.getTurnTimeLeft());

	    System.out.println("List of players in room :\n"+room.getPlayers());
	    
	    List<PlayerDTO> filteredPlayers = new ArrayList<>();
	    for (PlayerDTO p : room.getPlayers()) {
	        PlayerDTO copy = new PlayerDTO();
	        copy.setId(p.getId());
	        copy.setUsername(p.getUsername());
	        copy.setPoints(p.getPoints());

	        if (p.getId() == requestingPlayerId) {
	            // ✅ Send full hand only to this player
	            copy.setHand(p.getHand());
	        } else {
	            // ✅ Hide other players' cards
	            copy.setHand(Collections.emptyList());
	        }
	        copy.setHandCount(p.getHand() != null ? p.getHand().size() : 0);

	        filteredPlayers.add(copy);
	    }
	    filtered.setPlayers(filteredPlayers);
	    return filtered;
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
