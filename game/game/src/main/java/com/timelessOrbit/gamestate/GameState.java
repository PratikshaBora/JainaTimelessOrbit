package com.timelessOrbit.gamestate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.timelessOrbit.repository.PlayerScoreRepository;

@Component // ✅ makes GameState managed by Spring
public class GameState {

	@Autowired
	SimpMessagingTemplate messagingTemplate;
	@Autowired
	PlayerScoreRepository repository;

	List<Player> waitingPlayers = new ArrayList<Player>();
	List<GameRoom> gameRooms = new ArrayList<GameRoom>();
	private int nextPlayerId = 0; // global counter

	private List<PlayerScore> pastScores = new ArrayList<>();

	long firstPlayerJoinTime = 0;
	private final long timeoutMillis = 10000;// 2 * 60 * 1000; // configurable later

	public Player getCurrentPlayer(int roomId) {
		return gameRooms.get(roomId).getCurrentPlayer();
	}

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
		PlayerDTO dto = new PlayerDTO(player.getId(), player.getUsername(), player.getPoints(),
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
		room.setRepo(repository);
		int roomId = gameRooms.size(); // simple auto-increment
		room.setId(roomId);

		// Random allocation of players.
		Collections.shuffle(waitingPlayers);
		// Assign players from waiting list
		for (int i = 0; i < count; i++) {
			if (!waitingPlayers.isEmpty()) {
				Player p = waitingPlayers.remove(0);
				p.setRoomId(roomId); // assign room number
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

	public GameRoomDTO convertToDTO(GameRoom room) {
		GameRoomDTO dto = new GameRoomDTO();
		dto.setRoomId(room.getId());

		// Convert each Player to PlayerDTO
		List<PlayerDTO> playerDTOs = room.players.stream().map(p -> {
			return new PlayerDTO(p.getId(), p.getUsername(), p.getPoints(), new ArrayList<>(p.getHand()));
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

//	public Player checkAndHandleGameOver(int roomId) {
//	    GameRoom room = gameRooms.get(roomId);
//	    // 1. Check if game is over
//	    if (!room.isGameOver()) {
//	        return null; // game still running
//	    }
//	    // 2. Determine winner
//	    Player winner = room.getWinner();
//	    // 3. Announce winner (server-side log)
//	    System.out.println("🏁 Game Over in Room " + roomId +". Winner: " + winner.getUsername());
//	    // 4. Cleanup room
//	    endRoom(roomId);
//	    // 5. Return winner to caller (UI, network, etc.)
//	    return winner;
//	}

	// Access past scores
	public List<PlayerScore> getPastScores() {
		return repository.findAll();
	}

	public void playCard(int roomId, int playerId, Card card) {
		GameRoom room = gameRooms.get(roomId);
		// Find the player inside the room
		Player player = room.players.get(playerId);
		System.out.println("(Inside game state => Player : " + player.getUsername());
		for (Player p : room.players) {
			if (p.getId() == playerId) {
				player = p;
				break;
			}
		}
		room.playCard(player, card);
		GameRoomDTO dto = convertToDTO(room);
		messagingTemplate.convertAndSend("/topic/game/" + roomId, dto);
		if (room.winner != null && room.playerScore != null) {
			System.out.println("🎉 Winner is: " + room.winner.getUsername());
			messagingTemplate.convertAndSend("/topic/scoreboard", room.playerScore);
		}
	}

	public void drawCards(int roomId, GameMove move) {
		GameRoom room = gameRooms.get(roomId);
		// Find the player inside the room
		Player player = room.players.stream().filter(p -> p.getId() == move.getPlayerId()).findFirst().get(); // room.players.get(move.getPlayerId());
		System.out.println("(Inside game state => Player : " + player.getUsername());
		room.drawCards(player);
		GameRoomDTO dto = convertToDTO(room);
		messagingTemplate.convertAndSend("/topic/game/" + roomId, dto);
		if (room.winner != null && room.playerScore != null) {
			System.out.println("🎉 Winner is: " + room.winner.getUsername());
			messagingTemplate.convertAndSend("/topic/scoreboard", room.playerScore);
		}
	}

	public List<GameRoom> getGameRooms() {
		// TODO Auto-generated method stub
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

	public void endRoom(int roomId) {
		GameRoom room = gameRooms.get(roomId);
//	    List<PlayerScore> scores = new ArrayList<>();
//	    
//	    room.setEndedAt(LocalDateTime.now()); // ✅ capture end time
//	    long time = Duration.between(room.getCreatedAt(), room.getEndedAt()).getSeconds();
//	    room.setActiveRoomTime(time);
//	    if (room != null) {
//	        for (Player p : room.getPlayers()) {
//	            int points = room.calculatePoints(p);
//	            PlayerScore score = new PlayerScore(
//	                p.getId(),
//	                p.getUsername(),
//	                p.getMobileNumber(),
//	                points,
//	                roomId,
//	                time
//	            );
//	            pastScores.add(score);
//	            scores.add(score); // return snapshot for broadcast
//	            repository.save(score);	// adding score details into database 
//	        }
//	        System.out.println("Room " + roomId + " ended. Scores saved.");
//	    }
//	    System.out.println(scores);
//	    messagingTemplate.convertAndSend("/topic/scoreboard", scores);
		gameRooms.remove(roomId); // mark as ended
	}
}
