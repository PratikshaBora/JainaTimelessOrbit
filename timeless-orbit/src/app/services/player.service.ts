import { Injectable } from '@angular/core';
import { MessagePayload } from '../models/message-payload';
import { WebsocketService } from './websocket.service';

@Injectable({
  providedIn: 'root'
})
export class PlayerService {
  private players: MessagePayload[] = [];
  private currentUser!: MessagePayload;

  constructor(private wsService: WebsocketService) {}

  addOrGetPlayer(username: string, password: string): MessagePayload {
    let existingPlayer = this.players.find(p => p.username === username);
    if (!existingPlayer) {
      existingPlayer = { username, score: 0 };
      this.players.push(existingPlayer);
    }
    this.currentUser = existingPlayer;

    // Notify backend lobby join
    this.wsService.joinLobby(username);

    return existingPlayer;
  }

  updateScore(username: string, points: number) {
    const player = this.players.find(p => p.username === username);
    if (player) {
      player.score = (player.score ?? 0) + points;

      // Sync score update with backend
      this.wsService.sendMessage({
        type: 'UPDATE_SCORE',
        payload: { username, score: player.score }
      });
    }
  }

  getLeaderboard(): MessagePayload[] {
  return [...this.players] // clone
    .sort((a, b) => (b.score ?? 0) - (a.score ?? 0))
    .slice(0, 5);
}

  getCurrentUser(): MessagePayload {
    return this.currentUser;
  }

  setPlayersFromServer(players: MessagePayload[]) {
    this.players = players;
  }
}
