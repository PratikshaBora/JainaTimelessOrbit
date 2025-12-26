package com.timelessOrbit.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class LobbyController {

	// In-memory list of players (replace with DB or service later)
	private final List<String> lobbyPlayers = new ArrayList<>();

	/**
     * Handle player joining the lobby.
     * @param playerName name of the player joining
     * @return updated list of players
     */
    @MessageMapping("/joinLobby")
    @SendTo("/topic/lobby")
    public List<String> joinLobby(String playerName) {
        if (!lobbyPlayers.contains(playerName)) {
            lobbyPlayers.add(playerName);
        }
        return new ArrayList<>(lobbyPlayers);
    }

    /**
     * Handle player leaving the lobby.
     * @param playerName name of the player leaving
     * @return updated list of players
     */
    @MessageMapping("/leaveLobby")
    @SendTo("/topic/lobby")
    public List<String> leaveLobby(String playerName) {
        lobbyPlayers.remove(playerName);
        return new ArrayList<>(lobbyPlayers);
    }

    /**
     * Optional: broadcast current lobby state (e.g. countdown, ready status).
     */
    @MessageMapping("/lobbyStatus")
    @SendTo("/topic/lobby")
    public List<String> lobbyStatus() {
        return new ArrayList<>(lobbyPlayers);
    }

}
