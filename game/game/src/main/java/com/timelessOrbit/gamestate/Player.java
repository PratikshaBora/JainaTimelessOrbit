package com.timelessOrbit.gamestate;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name="players")
public class Player {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	private String username;
	private String mobileNumber; // optional if you need it
	private int points;
//	private int roomId;

	// --- Relation: One player has many cards in hand ---
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id") // foreign key in cards table
    public List<Card> hand = new ArrayList<>();

	private int handCount;
	private boolean saidJaiJinendra;
	private boolean penalty;
	private int winningBonus;

	// --- Constructors ---
	public Player() {
		this.points = 0;
		this.hand = new ArrayList<>();
		this.handCount = 0;
		this.saidJaiJinendra = false;
	}

	public Player(String username, String mobileNumber) {
        this.username = username;
        this.mobileNumber = mobileNumber;
        this.points = 0;
        this.hand = new ArrayList<>();
        this.saidJaiJinendra = false;
        this.penalty = false;
    }

//	public Player(int id, String username, int points, List<Card> hand) {
//		this.id = id;
//		this.username = username;
//		this.points = points;
//		// Defensive copy: ensures Player has its own list
//		this.hand = (hand != null) ? new ArrayList<>(hand) : new ArrayList<>();
//		this.handCount = this.hand.size(); // ✅ reflect actual hand size
//		this.saidJaiJinendra = false; // ✅ initialize consistently
//		this.penalty = false;
//	}

	// --- Getters & Setters ---
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
	}

//	public int getRoomId() {
//		return roomId;
//	}
//
//	public void setRoomId(int roomId) {
//		this.roomId = roomId;
//	}

	public List<Card> getHand() {
		return hand;
	}

	public void setHand(List<Card> hand) {
		this.hand = hand != null ? new ArrayList<>(hand) : new ArrayList<>();
		this.handCount = this.hand.size();
	}

	public int getHandCount() {
		return this.handCount = this.hand.size();
	}

	public int getWinningBonus() {
		return winningBonus;
	}

	public void setWinningBonus(int winningBonus) {
		this.winningBonus = winningBonus;
	}

	// --- Hand management methods ---
	/** Draw cards from draw pile */
	public void drawCards(List<Card> cards) {
		if (cards != null && !cards.isEmpty()) {
			this.hand.addAll(cards);
			this.handCount = this.hand.size(); // ✅ update count
		}
	}

	/** Discard a single card */
	public void discardCard(Card card) {
		if (card != null && this.hand.remove(card)) {
			this.handCount = this.hand.size(); // ✅ update count
		}
	}

	public boolean isSaidJaiJinendra() {
		return saidJaiJinendra;
	}

	public void setSaidJaiJinendra(boolean value) {
		this.saidJaiJinendra = value;
	}
	public boolean isPenalty() {
		return penalty;
	}
	public void setPenalty(boolean penalty) {
		this.penalty = penalty;
	}

	@Override
	public String toString() {
		return "Player [id=" + id + ", username=" + username + ", points=" + points + ", handCount=" + handCount
				+ ", hand=" + hand + "]";
	}

	public boolean removeCard(Card card) {
	    // Try to remove the card
	    boolean removed = hand.removeIf(c -> c.aara == card.aara && c.dwar == card.dwar);

	    // Return whether removal succeeded
	    return removed;
	}


	public void addCard(Card c) {
		hand.add(c);
		System.out.println("card count after draw : " + getHandCount());
	}
}