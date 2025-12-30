package com.timelessOrbit.gamestate;

import java.util.List;

public class PlayerDTO {
	private String username;
    private int points;
    private List<Card> hand;  // ✅ each player’s in-hand cards
}
