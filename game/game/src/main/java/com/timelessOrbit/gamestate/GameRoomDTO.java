package com.timelessOrbit.gamestate;

import java.util.List;

public class GameRoomDTO {
	 private int roomId;
	    private List<PlayerDTO> players;
	    private List<Card> discardPile;
	    private List<Card> drawPile;
	    // add other metadata if needed
}
