package com.timelessOrbit.gamestate;

public class GameMove {
    private int roomId;
    private int playerId;
    private Card card;

    public GameMove() {}

    public GameMove(int roomId, int playerId, Card card) {
        this.roomId = roomId;
        this.playerId = playerId;
        this.card = card;
    }

    public int getRoomId() {
        return roomId;
    }
    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getPlayerId() {
        return playerId;
    }
    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public Card getCard() {
        return card;
    }
    public void setCard(Card card) {
        this.card = card;
    }

    @Override
    public String toString() {
        return "GameMove [roomId=" + roomId + ", playerId=" + playerId + ", card=" + card + "]";
    }
}