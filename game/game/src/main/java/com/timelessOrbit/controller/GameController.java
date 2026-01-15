package com.timelessOrbit.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.timelessOrbit.gamestate.GameMove;
import com.timelessOrbit.gamestate.GameRoom;
import com.timelessOrbit.gamestate.GameRoomDTO;
import com.timelessOrbit.gamestate.GameState;
import com.timelessOrbit.gamestate.GameUpdate;
import com.timelessOrbit.gamestate.Player;
import com.timelessOrbit.gamestate.PlayerScore;
import com.timelessOrbit.gamestate.GameEngine;
import com.timelessOrbit.gamestate.Card;
import com.timelessOrbit.gamestate.AaraChangeMove;


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
    	System.out.println("inside controller for play card");
    	
//    	if(!gameState.getGameRooms().contains(move.getRoomId())) return;
    	
    	GameRoom room = gameState.getGameRooms().get(move.getRoomId());
    	if(gameState.getGameRooms().contains(room)) {
    	room.setGameEngine(room);

        // Apply move (validate inside GameState/Engine as needed)
        gameState.playCard(move.getRoomId(), move.getPlayerId(), move.getCard());

//        GameRoomDTO dto = gameState.convertToDTO(room);
////        System.out.println("current discard pile card : "+dto.getDiscardPile().get(dto.getDiscardPile().size()-1));
//        messagingTemplate.convertAndSend("/topic/game/" + roomId, dto);
//        if (room.winner != null && room.playerScore != null) {
//	        System.out.println("🎉 Winner is: " + room.winner.getUsername());
//	        messagingTemplate.convertAndSend("/topic/scoreboard", room.playerScore);
//	    }
        }
    }
    
    @MessageMapping("/game/{roomId}/draw")
    public void drawCard(@DestinationVariable int roomId, GameMove move) {
//    	if(!gameState.getGameRooms().contains(move.getRoomId())) return;
    	GameRoom room = gameState.getGameRooms().get(move.getRoomId());
        room.setGameEngine(room);
        gameState.drawCards(roomId,move);
//        GameRoomDTO dto = gameState.convertToDTO(room);
//        messagingTemplate.convertAndSend("/topic/game/" + roomId, dto);
//        if (room.winner != null && room.playerScore != null) {
//	        System.out.println("🎉 Winner is: " + room.winner.getUsername());
//	        messagingTemplate.convertAndSend("/topic/scoreboard", room.playerScore);
//	    }
    }
    @MessageMapping("/game/{roomId}/jaiJinendra")
    public void jaiJinendra(@DestinationVariable int roomId, GameMove move) {
        GameRoom room = gameState.getGameRooms().get(roomId);
        if (room != null) {
            // Mark the player as having said JJ
            room.setSaidJaiJinendra(true);

            // Create a DTO or simple message
            String message = move.getPlayerId() + " has declared Jai Jinendra!";

            // Broadcast to all players in the room
            messagingTemplate.convertAndSend("/topic/game/" + roomId + "/jaiJinendra", message);
        }
    }
    @GetMapping("/room/{id}")
    public GameRoomDTO getRoom(@PathVariable int id) {
        GameRoom room = gameState.getGameRooms().get(id);
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        return room.toDTO(); // convert to GameRoomDTO
    }
}