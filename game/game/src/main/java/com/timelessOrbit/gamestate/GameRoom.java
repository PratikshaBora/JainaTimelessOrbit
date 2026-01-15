package com.timelessOrbit.gamestate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.timelessOrbit.repository.PlayerScoreRepository;

public class GameRoom {
	private int id;
	public List<Player> players;
	public List<Card> drawPile;
	public List<Card> discardPile;
	public List<PlayerScore> playerScore;
	private Aara currentAara;
	public int currentPlayerIndex;

	public Player winner;

	private LocalDateTime createdAt; // ✅ when room is created
	private LocalDateTime endedAt; // ✅ when room ends
	private long activeRoomTime;
	public boolean clockwise;
	public boolean saidJaiJinendra;
	private GameEngine engine;
	private GameRoomDTO gameDTO = null;

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

	public void prepare_card() {
		for (Aara a : Aara.values()) {
			for (Dwar d : Dwar.values()) {
				if (d == Dwar.COLOR_CHANGE && (a == Aara.SECOND || a == Aara.FOURTH || a == Aara.SIXTH))
					continue;
				if (d == Dwar.COLOR_CHANGE_ADD4 && (a == Aara.FIRST || a == Aara.THIRD || a == Aara.FIFTH))
					continue;

				drawPile.add(new Card(a, d));
			}
		}
		int x = 1;
		for (Card card : drawPile) {
			System.out.println(x++ + " : " + card);
		}
	}

	public void distribute() {
		Collections.shuffle(drawPile);

		// Deal 5 cards one by one in rotation
		for (int round = 0; round < 5; round++) {
			for (Player p : players) {
				p.getHand().add(drawPile.remove(0));
			}
		}
		discardPile.add(drawPile.remove(0));
		setCurrentAara(discardPile.get(discardPile.size() - 1).aara);
		players.forEach(p -> {
			System.out.println("Cards in " + p.getUsername() + " hand : ");
			p.getHand().forEach(card -> System.out.println(card));
		});
	}

	public void playCard(Player player, Card card) {
		System.out.println("inside game room: " + card);
		System.out.println("current player : " + player.getUsername());
		Card top = discardPile.get(discardPile.size() - 1);
		
		// Only allow if it's the player's turn
		if (player != getCurrentPlayer()) {
			System.out.println("player not matched");
		} else {
			if (player.getHand().size() == 1 && saidJaiJinendra == true) {
				if(engine.isValidMove(card, top)) {
					discardPile.add(card);
					boolean success = player.removeCard(card);
					if(success == false) {
						winner = resolveEndgame();
					}
				}
				else {
					drawCards(player);
				}
			} else {

			}
		}
		
		System.out.println("Checking : " + card + " with " + top);

		if (engine.isValidMove(card, top)) {
			boolean success = player.removeCard(card);
			if (success == false) {
				winner = resolveEndgame();
			} else {
				System.out.println("In hand cards : " + player.hand.size());
				System.out.println("current hand card no : " + player.getHandCount());
				discardPile.add(card);
				setCurrentAara(discardPile.get(discardPile.size() - 1).aara);
				System.out.println("current top discard : " + discardPile.get(discardPile.size() - 1));
				engine.applyAction(card);
				engine.nextPlayer();
			}
		}

	}

	public Player drawCards(Player player) {
		System.out.println("Subscribed player : " + player.getUsername());
		System.out.println("Current player : " + getCurrentPlayer().getUsername());
		if (player != getCurrentPlayer()) {
			System.out.println("player not matched");
		}
		if (!drawPile.isEmpty()) {
			Card c = drawPile.remove(0);
			// add to in hand
			player.addCard(c);
			if (!engine.isValidMove(c, discardPile.get(discardPile.size() - 1))) {
				engine.nextPlayer();
			}
			System.out.println("remaining cards: " + drawPile.size());
		} else {
			System.out.println("⚠️ Draw pile empty, cannot draw. Endgame should be resolved.");
			return resolveEndgame();
		}
		return null;
	}

	public String removePlayer(int pid) {
		for (Player player : new ArrayList<>(players)) { // avoid ConcurrentModification
			if (player.getId() == pid) {
				players.remove(player); // ✅ remove by object, not index
				return player.getUsername() + " left the room.";
			}
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

	// Endgame rule: lowest hand count wins, score = sum of others’ points
	public Player resolveEndgame() {
		Player winner = null;
		setEndedAt(LocalDateTime.now()); // ✅ capture end time
		activeRoomTime = Duration.between(getCreatedAt(), getEndedAt()).getSeconds();
		setActiveRoomTime(activeRoomTime);
		int lowestHandPoints = Integer.MAX_VALUE;

		for (Player p : players) {
			int points = calculatePoints(p);
			if (points < lowestHandPoints) {
				lowestHandPoints = points;
				p.setPoints(points);
				winner = p;
			}
		}

		int bonus = 0;
		for (Player p : players) {
			if (p != winner) {
				for (Card c : p.getHand()) {
					bonus += c.getPointValue();
				}
			}
		}
		if (winner != null) {
			winner.setPoints(winner.getPoints() + bonus); // ✅ use Player.setPoints()
			for (Player p : getPlayers()) {
				PlayerScore score = new PlayerScore(p.getId(), p.getUsername(), p.getMobileNumber(), p.getPoints(),
						p.getRoomId(), activeRoomTime);
				repo.save(score);
				playerScore.add(score);
			}
		}
		System.out.println("🏁 Endgame triggered. Winner: " + winner.getUsername() + " with bonus " + bonus);

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
