package com.timelessOrbit.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.timelessOrbit.gamestate.GameMove;
import com.timelessOrbit.gamestate.GameRoom;
import com.timelessOrbit.gamestate.GameRoomDTO;
import com.timelessOrbit.gamestate.GameState;
import com.timelessOrbit.gamestate.Player;
import com.timelessOrbit.gamestate.PlayerDTO;
import com.timelessOrbit.gamestate.RejoinRequest;

@Controller
@CrossOrigin(origins = "http://localhost:8100",allowCredentials = "true")
public class GameController {
	
	@Autowired
    private GameState gameState = new GameState();
	@Autowired
    private SimpMessagingTemplate messagingTemplate;

    // --- Lobby Management ---
    @MessageMapping("/join")
    @SendTo("/topic/lobby")
    public List<Player> join(Player player) {
    	System.out.println("Player : "+player.getUsername());
        gameState.addPlayer(player);
        return new ArrayList<Player>(gameState.getPlayers());
    }
    
    @MessageMapping("/leaveLobby")
    @SendTo("/topic/lobby")
    public List<Player> leaveLobby(Player player) {
        gameState.removePlayer(player.getId());
        return new ArrayList<>(gameState.getPlayers());
    }

    @MessageMapping("/lobbyStatus")
    @SendTo("/topic/lobby")
    public List<Player> lobbyStatus() {
        return new ArrayList<>(gameState.getPlayers());
    }
    
// --- Core gameplay: always broadcast GameRoomDTO to /topic/game/{roomId} ---
    @MessageMapping("/game/{roomId}/play")
    public void playCard(@DestinationVariable int roomId, GameMove move) {
//    	if(!gameState.getGameRooms().contains(move.getRoomId())) return;
        gameState.playCard(move.getRoomId(), move.getPlayerId(), move.getCard());
    }
    
    @MessageMapping("/game/{roomId}/draw")
    public void drawCard(@DestinationVariable int roomId, GameMove move) {
//    	if(!gameState.getGameRooms().contains(move.getRoomId())) return;
        gameState.drawCards(roomId,move,false);
    }
    @MessageMapping("/game/{roomId}/jaiJinendra")
    public void jaiJinendra(@DestinationVariable int roomId, GameMove move) {
        GameRoom room = gameState.getGameRooms().get(roomId);
        if (room != null) {
            // Mark the player as having said JJ
            room.setSaidJaiJinendra(true);
            // Create a DTO or simple message
//            String message = room.players.get(move.getPlayerId()).getUsername() + " has declared Jai Jinendra!";
            // Broadcast to all players in the room
            System.out.println(room.players.get(move.getPlayerId()).getUsername()+" declared jai jinendra");
          // messagingTemplate.convertAndSend("/topic/game/" + roomId + "/jaiJinendra", message);
        }
    }
    @MessageMapping("/game/{roomId}/autoPenaltyDraw")
    public void autoPenaltyDraw(@DestinationVariable int roomId, GameMove move) {
    	System.out.println("inside auto penalty method");
    	gameState.drawCards(roomId,move,true);
    }
    @MessageMapping("/app/game/{roomId}/jjTimeoutPenalty")
    public void jjTimeoutPenalty(@DestinationVariable int roomId, GameMove move) {
    	System.out.println("inside jai jinendra penalty method");
    	gameState.drawCards(roomId,move,true);
    }
    @GetMapping("/room/{id}")
    public GameRoomDTO getRoom(@PathVariable int id) {
        GameRoom room = gameState.getGameRooms().get(id);
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        return room.toDTO(); // convert to GameRoomDTO
    }
    @MessageMapping("/game/{roomId}/leave")
    public void leaveRoom(@DestinationVariable int roomId, PlayerDTO player) {
        GameRoom room = gameState.getRoom(roomId);
        if (room == null) {
            System.out.println("Room not found: " + roomId);
            return;
        }

        // Remove player from room
        String result = room.removePlayer(player.getId());
        System.out.println("Player leaving room: " + result);

        // Broadcast updated room state
        GameRoomDTO dto = gameState.convertToDTO(room);
        messagingTemplate.convertAndSend("/topic/game/" + roomId, dto);

        // If room becomes empty, end it
        if (room.getPlayers().isEmpty()) {
            gameState.endRoom(roomId);
            messagingTemplate.convertAndSend("/topic/game/" + roomId + "/end",
                "Room " + roomId + " has ended. Returning to home.");
        }
    }
    @MessageMapping("/rejoin")
    public void rejoin(RejoinRequest request) {
        gameState.rejoin(request);
    }
}