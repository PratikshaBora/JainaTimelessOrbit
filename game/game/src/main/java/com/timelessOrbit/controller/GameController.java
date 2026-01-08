package com.timelessOrbit.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.timelessOrbit.gamestate.GameMove;
import com.timelessOrbit.gamestate.GameRoom;
import com.timelessOrbit.gamestate.GameState;
import com.timelessOrbit.gamestate.GameUpdate;
import com.timelessOrbit.gamestate.Player;
import com.timelessOrbit.gamestate.PlayerScore;
import com.timelessOrbit.gamestate.GameEngine;
import com.timelessOrbit.gamestate.Card;
import com.timelessOrbit.gamestate.ColorChangeMove;

@Controller
@CrossOrigin(origins = "http://localhost:8100",allowCredentials = "true")
public class GameController {
	
	@Autowired
    private GameState gameState = new GameState();

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


    // --- Core gameplay ---
    public List<Player> playRoom() {
    	System.out.println("Creating a play room");
        gameState.checkRoom();
        return new ArrayList<Player>(gameState.getRoomPlayers());
    }
//    @MessageMapping("/startGame")
//    @SendTo("/topic/game")
//    public GameUpdate startGame() {
//        if (gameState.getPlayers().size() < 2) {
//            return new GameUpdate(-1, false, null);
//        }
//        GameRoom room = gameState.createRoom(gameState.getPlayers());
//        return new GameUpdate(room.getId(), true, null);
//    }
    @MessageMapping("/playCard")
    @SendTo("/topic/game")
    public GameUpdate playCard(GameMove move) {
        boolean success = gameState.playCard(move.getRoomId(), move.getPlayerId(), move.getCard());
        Player winner = gameState.checkAndHandleGameOver(move.getRoomId());
        return new GameUpdate(move.getRoomId(), success, winner);
    }
    @MessageMapping("/drawCard")
    @SendTo("/topic/game")
    public GameUpdate drawCard(GameMove move) {
        GameRoom room = gameState.getGameRooms().get(move.getRoomId());
        Player player = room.getPlayers().get(move.getPlayerId());

        drawCards(player, room, 1);
        return new GameUpdate(move.getRoomId(), true, null);
    }
    // --- New actions ---
    @MessageMapping("/pickDiscard")
    @SendTo("/topic/game")
    public GameUpdate pickDiscard(GameMove move) {
        GameRoom room = gameState.getGameRooms().get(move.getRoomId());
        Player player = room.getPlayers().get(move.getPlayerId());
        GameEngine engine = new GameEngine(room);

        if (!room.discardPile.isEmpty()) {
            Card card = room.discardPile.remove(room.discardPile.size() - 1);

            boolean success;
            if (engine.isValidMove(card, room.getTopDiscard())) {
                success = room.playCard(player, card);
            } else {
                // Just add to hand; success = false because the card couldn't be played
                player.getHand().add(card);
                success = false;
            }
            return new GameUpdate(move.getRoomId(), success, null);
        }
        return new GameUpdate(move.getRoomId(), false, null);
    }

    @MessageMapping("/jaiJinendra")
    @SendTo("/topic/game")
    public GameUpdate jaiJinendra(GameMove move) {
        GameRoom room = gameState.getGameRooms().get(move.getRoomId());
        Player player = room.getPlayers().get(move.getPlayerId());
        GameEngine engine = new GameEngine(room);

        // Mark declaration to avoid auto-penalty in GameEngine
        player.setSaidJaiJinendra(true);
        if (player.getHand().size() == 1) {
            Card lastCard = player.getHand().get(0);
            boolean canPlay = engine.isValidMove(lastCard, room.getTopDiscard());
            if (canPlay) {
                boolean success = gameState.playCard(move.getRoomId(), move.getPlayerId(), lastCard);
                Player winner = gameState.checkAndHandleGameOver(move.getRoomId());
                return new GameUpdate(move.getRoomId(), success, winner);
            } else {
                // Penalty: draw 1
                drawCards(player, room, 1);
                return new GameUpdate(move.getRoomId(), false, null);
            }
        }
        return new GameUpdate(move.getRoomId(), false, null);
    }


    @MessageMapping("/penaltyDraw")
    @SendTo("/topic/game")
    public GameUpdate penaltyDraw(GameMove move) {
        GameRoom room = gameState.getGameRooms().get(move.getRoomId());
        Player player = room.getPlayers().get(move.getPlayerId());
        drawCards(player, room, 1);
        return new GameUpdate(move.getRoomId(), true, null);
    }

    @MessageMapping("/jjPenalty")
    @SendTo("/topic/game")
    public GameUpdate jjPenalty(GameMove move) {
        GameRoom room = gameState.getGameRooms().get(move.getRoomId());
        Player player = room.getPlayers().get(move.getPlayerId());
        drawCards(player, room, 1);
        return new GameUpdate(move.getRoomId(), true, null);
    }

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/endgame")
    @SendTo("/topic/game")
    public GameUpdate endgame(GameMove move) {
        GameRoom room = gameState.getGameRooms().get(move.getRoomId());
        Player winner = gameState.resolveEndgame(room);

        // ✅ Let GameState handle score calculation
        List<PlayerScore> scores = gameState.endRoom(move.getRoomId());

        // ✅ Controller only broadcasts
        messagingTemplate.convertAndSend("/topic/scoreboard", scores);

        return new GameUpdate(move.getRoomId(), true, winner);
    }
    @MessageMapping("/drawTwo")
    @SendTo("/topic/game")
    public GameUpdate drawTwo(GameMove move) {
        GameRoom room = gameState.getGameRooms().get(move.getRoomId());
        Player player = room.getPlayers().get(move.getPlayerId());
        
        new GameEngine(room).drawToPlayer(player, 2);
     // Optionally broadcast updated room state
        messagingTemplate.convertAndSend("/topic/game/" + room.getId(), room.toDTO());
//      drawCards(player, room, 2);
        return new GameUpdate(move.getRoomId(), true, null);
    }
    @MessageMapping("/drawFour")
    @SendTo("/topic/game")
    public GameUpdate drawFour(GameMove move) {
        GameRoom room = gameState.getGameRooms().get(move.getRoomId());
        Player player = room.getPlayers().get(move.getPlayerId());
        drawCards(player, room, 4);
        return new GameUpdate(move.getRoomId(), true, null);
    }
    // Utility
    public void drawCards(Player player, GameRoom room, int count) {
        for (int i = 0; i < count && !room.drawPile.isEmpty(); i++) {
            player.getHand().add(room.drawPile.remove(0));
        }
    }
    // --- New special card actions ---
    @MessageMapping("/skipTurn")
    @SendTo("/topic/game")
    public GameUpdate skipTurn(GameMove move) {
        GameRoom room = gameState.getGameRooms().get(move.getRoomId());
        GameEngine engine = new GameEngine(room);
        
        boolean success = engine.skipTurn(move.getPlayerId());
     // Broadcast updated room state
        messagingTemplate.convertAndSend("/topic/game/" + room.getId(), room.toDTO());
        return new GameUpdate(move.getRoomId(), success, null);
    }
    @MessageMapping("/reverseTurn")
    @SendTo("/topic/game")
    public GameUpdate reverseTurn(GameMove move) {
        GameRoom room = gameState.getGameRooms().get(move.getRoomId());
        GameEngine engine = new GameEngine(room);
        boolean success = engine.reverseTurn();
     // Broadcast updated room state
        messagingTemplate.convertAndSend("/topic/game/" + room.getId(), room.toDTO());
        return new GameUpdate(move.getRoomId(), success, null);
    }
    @MessageMapping("/colorChange")
    @SendTo("/topic/game")
    public GameUpdate colorChange(ColorChangeMove move) {
        GameRoom room = gameState.getRoom(move.getRoomId());
        GameEngine engine = new GameEngine(room);
        
        engine.applyColorChange(move.getColor()); // chosen aara
     // Broadcast updated room state
        messagingTemplate.convertAndSend("/topic/game/" + room.getId(), room.toDTO());
        return new GameUpdate(move.getRoomId(), true, null);
    }
    @MessageMapping("/colorChangeDrawFour")
    @SendTo("/topic/game")
    public GameUpdate colorChangeDrawFour(ColorChangeMove move) {
        GameRoom room = gameState.getRoom(move.getRoomId());
        GameEngine engine = new GameEngine(room);
             
        Player nextPlayer = room.getNextPlayer(move.getPlayerId());
        engine.drawToPlayer(nextPlayer, 4);		// pick 4 from draw pile due to drawFour
        engine.applyColorChange(move.getColor()); // chosen aara
     // Optionally broadcast updated room state
        messagingTemplate.convertAndSend("/topic/game/" + room.getId(), room.toDTO());
        return new GameUpdate(move.getRoomId(), true, null);
    }
}