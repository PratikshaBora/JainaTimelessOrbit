package com.timelessOrbit.gamestate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.timelessOrbit.repository.GameRoomRepository;
import com.timelessOrbit.repository.PlayerRepository;
import com.timelessOrbit.repository.PlayerScoreRepository;

@Component // ✅ makes GameState managed by Spring
public class GameState {

	@Autowired
	SimpMessagingTemplate messagingTemplate;
	@Autowired
	PlayerScoreRepository playerScoreRepository;
	@Autowired
	GameRoomRepository gameRoomRepository;
	@Autowired
	PlayerRepository playerRepository; 

	List<Player> waitingPlayers = Collections.synchronizedList(new ArrayList<>());
	List<GameRoom> gameRooms = Collections.synchronizedList(new ArrayList<>());

	private List<PlayerScore> pastScores = new ArrayList<>();

	long firstPlayerJoinTime = 0;
	private final long timeoutMillis = 10000;// 2 * 60 * 1000; // configurable later

	public Player getCurrentPlayer(int roomId) {
		return gameRooms.get(roomId).getCurrentPlayer();
	}

	// --- Lobby management ---

	// When a player joins, they go into waitingPlayers.
	public void addPlayer(Player p) {
		System.out.println("player info received from frontend: " + p);

		// ✅ Create backend Player with assigned ID
		Player player = new Player();
//		player.setId(nextPlayerId++);
		player.setUsername(p.getUsername());
		player.setMobileNumber(p.getMobileNumber());
		
		
		System.out.println("Populated player with id : " + player);

		waitingPlayers.add(player);
		playerRepository.save(player);
		System.out.println("No of waiting players : " + waitingPlayers.size());

		// ✅ Convert to DTO
		PlayerDTO dto = new PlayerDTO(player.getId(), player.getUsername(), player.getMobileNumber(),
				player.getPoints(),	new ArrayList<>(player.getHand()) // defensive copy
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
		System.out.println("inside scheduled : " + waitingPlayers.size());
		checkRoom(); // call your existing method
	}

	public void checkRoom() {
		System.out.println("waiting players for room allotment: " + waitingPlayers.size());
		long now = System.currentTimeMillis();

		while (!waitingPlayers.isEmpty()) {
			int size = waitingPlayers.size();

			if (size >= 4) {
				createRoom(4);
			} else if (size > 1 && (now - firstPlayerJoinTime) >= timeoutMillis) {
				createRoom(size);
			} else if (size == 1 && (now - firstPlayerJoinTime) >= timeoutMillis) {
				messagingTemplate.convertAndSend("/topic/lobbyStatus", "Only one player remaining, auto-adding player");
			} else {
				break; // wait for more players or timeout
			}
		}
	}

	public void createRoom(int count) {
		GameRoom room = new GameRoom();
		gameRoomRepository.save(room);
		int roomId = gameRooms.size(); // simple auto-increment
		room.setId(roomId);

		// Random allocation of players.
		Collections.shuffle(waitingPlayers);
		// Assign players from waiting list
		for (int i = 0; i < count; i++) {
			if (!waitingPlayers.isEmpty()) {
				Player p = waitingPlayers.remove(0);
				playerRepository.save(p);
				room.players.add(p);
			}
		}

		// Prepare and distribute cards
		room.prepareDeck();
		room.distributeCards(5);
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

	public GameRoomDTO convertToDTO(GameRoom room) {
		GameRoomDTO dto = new GameRoomDTO();
		dto.setRoomId(room.getId());

		// Convert each Player to PlayerDTO
		List<PlayerDTO> playerDTOs = room.players.stream().map(p -> {
			return new PlayerDTO(p.getId(), p.getUsername(), p.getMobileNumber(),
					p.getPoints(), new ArrayList<>(p.getHand()));
		}).toList();

		dto.setPlayers(playerDTOs);
		dto.setDiscardPile(new ArrayList<>(room.discardPile));
		dto.setDrawPile(new ArrayList<>(room.drawPile));

		// ✅ Add turn info
		dto.setCurrentPlayerId(room.getCurrentPlayer().getId());
		dto.setClockwise(room.clockwise);
		dto.setCurrentAara(room.getCurrentAara());
		dto.setWinner(room.winner);

		// ✅ Reset timer at start of each turn
		dto.setTurnTimeLeft(60);

		return dto;
	}

	// Access past scores
	public List<PlayerScore> getPastScores() {
		pastScores = playerScoreRepository.findAll();
		return pastScores;
	}

	public void playCard(int roomId, int playerId, Card card) {
		GameRoom room = gameRooms.get(roomId);
		if (room == null) {
			System.out.println("Room not found: " + roomId);
			return;
		}

		// Find player by ID
		Player player = room.getPlayers().stream().filter(p -> p.getId() == playerId).findFirst().orElse(null);

		if (player == null) {
			System.out.println("Player not found in room: " + playerId);
			return;
		}

		System.out.println("Inside game state => Player : " + player.getUsername());

		// Apply move
		room.playCard(player, card);

		// Broadcast updated room state
		GameRoomDTO dto = convertToDTO(room);
		messagingTemplate.convertAndSend("/topic/game/" + roomId, dto);

		// If winner exists, broadcast scoreboard
		if (room.winner != null && room.playerScore != null) {
			System.out.println("🎉 Winner is: " + room.winner.getUsername());
			messagingTemplate.convertAndSend("/topic/scoreboard", room.playerScore);
			// ⏳ Schedule auto-end broadcast
		}
	}

	public void drawCards(int roomId, GameMove move, boolean val) {
		GameRoom room = gameRooms.get(roomId);
		if (room == null) {
			System.out.println("Room not found: " + roomId);
			return;
		}

		Player player = room.getPlayers().stream().
				filter(p -> p.getId() == move.getPlayerId()).findFirst()
				.orElse(null);

		if (player == null) {
			System.out.println("Player not found: " + move.getPlayerId());
			return;
		}

		player.setPenalty(val);
		System.out.println("(Inside game state => Player : " + player.getUsername());

		room.drawCards(player);
		
		GameRoomDTO dto = convertToDTO(room);
		messagingTemplate.convertAndSend("/topic/game/" + roomId, dto);

		if (room.winner != null && room.playerScore != null) {
			System.out.println("🎉 Winner is: " + room.winner.getUsername());
			messagingTemplate.convertAndSend("/topic/scoreboard", room.playerScore);
			// ⏳ Schedule auto-end broadcast
		}
	}

	public List<GameRoom> getGameRooms() {
		return gameRooms;
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

	@Scheduled(fixedRate = 30000) // runs every 30 seconds
	public void scheduledEndRoomCheck() {
	    long now = System.currentTimeMillis();

	    for (GameRoom room : new ArrayList<>(gameRooms)) {
	        if (room.getEndedAt() != null) {
	            long elapsed = now - room.getEndedAt()
	                .atZone(java.time.ZoneId.systemDefault())
	                .toInstant().toEpochMilli();

	            if (elapsed >= 3 * 60 * 1000) { // 3 minutes
	                System.out.println("⏳ Auto-ending room " + room.getId());
	                messagingTemplate.convertAndSend(
	                    "/topic/game/" + room.getId() + "/end",
	                    "Room " + room.getId() + " has ended. Returning to home."
	                );
	                endRoom(room.getId()); // remove room
	            }
	        }
	    }
	}

	public void endRoom(int roomId) {
        gameRooms.removeIf(r -> r.getId() == roomId);
        gameRoomRepository.deleteById(roomId);
    }

	public void rejoin(RejoinRequest request) {
	
		// 1. Find player
        Player player = playerRepository.findByUsernameAndMobileNumber(
            request.getUsername(), request.getMobileNumber()
        );
        if (player == null) {
            throw new RuntimeException("Player not found");
        }

        // 2. Find room
        GameRoom room = gameRoomRepository.findById(request.getRoomId())
            .orElseThrow(() -> new RuntimeException("Room not found"));

        // 3. Rehydrate transient runtime fields
        room.setGameEngine(room);
    
        // 4. Build DTO snapshot
        GameRoomDTO dto = room.toDTO();

        // 5. Broadcast updated state
        messagingTemplate.convertAndSend("/topic/game/" + room.getId(), dto);
	}
}
