package com.timelessOrbit.gamestate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.timelessOrbit.repository.PlayerScoreRepository;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "game_rooms")
public class GameRoom {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	// --- Relations ---
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id")
	public List<Player> players;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id")
	public List<Card> drawPile;

    // --- Runtime only ---
    @Transient
    public List<Card> discardPile;
	
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id")
	public List<PlayerScore> playerScore;
	
    @Enumerated(EnumType.STRING)
	private Aara currentAara;

    @Transient
	public int currentPlayerIndex=0;

    @Transient
    public Player winner;

	private LocalDateTime createdAt; // ✅ when room is created
	private LocalDateTime endedAt; // ✅ when room ends
	private long activeRoomTime;
	@Transient
	private GameRoomDTO gameDTO = null;
	public boolean clockwise;
	public boolean saidJaiJinendra;
	
	@Transient
	private GameEngine engine;
		
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "top_discard_card_id")
    private Card topDiscardCard;

	@Autowired
	PlayerScoreRepository repo;

	public void setRepo(PlayerScoreRepository repo) {
		this.repo = repo;
	}

	public GameRoom() {
		engine = new GameEngine(this);
		players = new ArrayList<>();
		drawPile = new ArrayList<>();
		discardPile = new ArrayList<>();
		playerScore = new ArrayList<>();
		currentPlayerIndex = 1;
		this.createdAt = LocalDateTime.now(); // ✅ capture creation time
		saidJaiJinendra = false;
		clockwise = true;
		winner = null;
	}

	public void setGameEngine(GameRoom room) {
		this.engine = new GameEngine(room);
	}

	public GameEngine getGameEngine() {
		return this.engine;
	}

	public GameRoomDTO getGameDTO() {
		return gameDTO;
	}

	public void setGameDTO(GameRoomDTO gameDTO) {
		this.gameDTO = gameDTO;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Aara getCurrentAara() {
		return currentAara;
	}

	public void setCurrentAara(Aara currentAara) {
		this.currentAara = currentAara;
	}

	public boolean isDrawPileEmpty() {
		return drawPile.isEmpty();
	}

	public Player getCurrentPlayer() {
		return players.get(currentPlayerIndex);
	}

	public LocalDateTime getCreatedAt() {
		return this.createdAt;
	}

	public void setEndedAt(LocalDateTime time) {
		this.endedAt = time;
	}

	public LocalDateTime getEndedAt() {
		return this.endedAt;
	}

	public void setActiveRoomTime(long activeTime) {
		this.activeRoomTime = activeTime;
	}

	public long getActiveRoomTime() {
		return this.activeRoomTime;
	}

	public void setSaidJaiJinendra(boolean b) {
		this.saidJaiJinendra = b;
	}

	public boolean getSaidJaiJinendra() {
		return this.saidJaiJinendra;
	}

	public Player getNextPlayer(int currentPlayerId) {
		int index = -1;
		for (int i = 0; i < players.size(); i++) {
			if (players.get(i).getId() == currentPlayerId) {
				index = i;
				break;
			}
		}
		if (index == -1)
			return null;

		int nextIndex;
		if (clockwise) {
			nextIndex = (index + 1) % players.size();
		} else {
			nextIndex = (index - 1 + players.size()) % players.size();
		}
		currentPlayerIndex = nextIndex;
		return players.get(nextIndex);
	}

	public boolean isGameOver() {
		return drawPile.isEmpty();
	}

	public List<Player> getPlayers() {
		return players;
	}

	public void nextTurn() {
		if (clockwise) {
			currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
		} else {
			currentPlayerIndex = (currentPlayerIndex - 1 + players.size()) % players.size();
		}
	}

	// --- Game logic methods ---
    public void prepareDeck() {
        drawPile.clear();
        for (Aara a : Aara.values()) {
            for (Dwar d : Dwar.values()) {
                drawPile.add(new Card(a, d));
            }
        }
        Collections.shuffle(drawPile);
    }

    public void distributeCards(int handSize) {
        for (int round = 0; round < handSize; round++) {
            for (Player p : players) {
                if (!drawPile.isEmpty()) {
                    p.addCard(drawPile.remove(0));
                }
            }
        }
        // First discard
        if (!drawPile.isEmpty()) {
            topDiscardCard = drawPile.remove(0);
            discardPile.add(topDiscardCard);
            currentAara = topDiscardCard.getAara();
        }
    }
    
	public void playCard(Player player, Card card) {
		System.out.println("inside game room: " + card);
		System.out.println("current player : " + player.getUsername());
		Card top = discardPile.get(discardPile.size() - 1);
		System.out.println("Jai Jinendra : " + getSaidJaiJinendra());

		// Only allow if it's the player's turn
		if (player != getCurrentPlayer()) {
			System.out.println("player not matched");
			return;
		}

		boolean isLastCard = player.getHand().size() == 1;

		// Special JJ case: player must declare before playing last card
		if (isLastCard && !saidJaiJinendra) {
			System.out.println("Penalty: player did not declare Jai Jinendra!");
			drawCards(player);
			return;
		}

		// Validate move
		if (engine.isValidMove(card, top)) {
			boolean removed = player.removeCard(card);

			if (removed) {
				discardPile.add(card);
				setCurrentAara(card.aara);
				System.out.println("current top discard : " + card);

				engine.applyAction(card);

				// Endgame check
				if (player.getHand().isEmpty()) {
					winner = resolveEndgame();
				} else {
					engine.nextPlayer();
				}
			} else {
				System.out.println("Card not found in hand!");
			}
		} else {
			System.out.println("Invalid move, drawing cards...");
			drawCards(player);
		}
	}

	public Player drawCards(Player player) {
		System.out.println("Subscribed player : " + player.getUsername());
		System.out.println("Current player : " + getCurrentPlayer().getUsername());

		if (player != getCurrentPlayer()) {
			System.out.println("Not this player's turn!");
			return null; // enforce turn order
		}

		if (!drawPile.isEmpty()) {
			Card c = drawPile.remove(0);
			player.addCard(c);

			if (!engine.isValidMove(c, discardPile.get(discardPile.size() - 1))) {
				engine.nextPlayer();
			}

			System.out.println("remaining cards: " + drawPile.size());
		} else {
			System.out.println("⚠️ Draw pile empty, resolving endgame.");
			return resolveEndgame();
		}
		return null;
	}

//	public String removePlayer(int pid) {
//		for (Player player : new ArrayList<>(players)) { // avoid ConcurrentModification
//			if (player.getId() == pid) {
//				players.remove(player); // ✅ remove by object, not index
//				return player.getUsername() + " left the room.";
//			}
//		}
//		return null;
//	}
	
	public String removePlayer(int playerId) {
	    Player toRemove = players.stream()
	        .filter(p -> p.getId() == playerId)
	        .findFirst()
	        .orElse(null);

	    if (toRemove != null) {
	        players.remove(toRemove);
	        return toRemove.getUsername() + " left the room.";
	    }
	    return null;
	}

	public int calculatePoints(Player p) {
		int sum = 0;
		for (Card c : p.getHand()) {
			sum += c.getPointValue();
		}
		return sum;
	}

	public Player resolveEndgame() {
//		Player winner = null;
		setEndedAt(LocalDateTime.now());
		activeRoomTime = Duration.between(getCreatedAt(), getEndedAt()).getSeconds();
		setActiveRoomTime(activeRoomTime);

		int lowestHandPoints = Integer.MAX_VALUE;

		// Step 1: Calculate points for each player
		for (Player p : players) {
			int points = calculatePoints(p);
			p.setPoints(points);
			if (points < lowestHandPoints) {
				lowestHandPoints = points;
				this.winner = p;
			}
		}

		// Step 2: Calculate bonus = sum of other players’ card values
		int bonus = players.stream().filter(p -> p != this.winner).flatMap(p -> p.getHand().stream())
				.mapToInt(Card::getPointValue).sum();

		// Step 3: Assign bonus to winner, 0 to others
		if (winner != null) {
			winner.setWinningBonus(bonus); // winner gets bonus
//	        winner.setPoints(winner.getPoints() + bonus); // add bonus to winner’s points

			playerScore.clear(); // avoid duplicates

			for (Player p : getPlayers()) {
				PlayerScore score = new PlayerScore(
					    p.getId(), p.getUsername(), p.getMobileNumber(),
					    p.getPoints(), activeRoomTime, this
					);
					repo.save(score);

				// Assign bonus field correctly
				if (p == winner) {
					score.setBonus(bonus);
				} else {
					score.setBonus(0);
				}

				repo.save(score);
				playerScore.add(score);
			}

			System.out.println("🏁 Endgame triggered. Winner: " + winner.getUsername() + " with bonus " + bonus);
		}

		return winner;
	}

	public GameRoomDTO toDTO() {
		GameRoomDTO dto = new GameRoomDTO();
		dto.setRoomId(this.id);
		// Convert each Player to PlayerDTO
		List<PlayerDTO> playerDTOs = new ArrayList<>();
		for (Player p : this.players) {
			PlayerDTO pdto = new PlayerDTO();
			pdto.setId(p.getId());
			pdto.setUsername(p.getUsername());
			pdto.setPoints(p.getPoints());
			pdto.setHand(new ArrayList<>(p.getHand())); // shallow copy of hand
			playerDTOs.add(pdto);
		}
		dto.setPlayers(playerDTOs);
		dto.setDrawPile(new ArrayList<>(this.drawPile));
		dto.setDiscardPile(new ArrayList<>(this.discardPile));
		dto.setCurrentPlayerId(this.players.get(currentPlayerIndex).getId());
		dto.setPlayerScore(new ArrayList<>(this.playerScore));
		System.out.println("current player : " + this.players.get(currentPlayerIndex).getUsername());
		dto.setWinner(this.winner);
		dto.setClockwise(this.clockwise);
		dto.setCurrentAara(this.currentAara);

		return dto;
	}

	@Override
	public String toString() {
		return "GameRoom [id=" + id + ", players=" + players + ", drawPile=" + drawPile + ", discardPile=" + discardPile
				+ ", currentAara=" + currentAara + ", currentPlayerIndex=" + currentPlayerIndex + ", clockwise="
				+ clockwise + "]";
	}
}
