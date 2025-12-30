import { Injectable } from '@angular/core';
import { MessagePayload } from '../models/message-payload';
import { WebsocketService } from './websocket.service';

@Injectable({
  providedIn: 'root'
})
export class PlayerService {
  private players: MessagePayload[] = [];
  private currentUser : MessagePayload | null=null;
  private nextId = 1; // NEW: auto-increment counter

  constructor(private wsService: WebsocketService) {
    // Subscribe to WebsocketService players$
    this.wsService.players$.subscribe(players => {
      this.players = players;
    });
  }

  addOrGetPlayer(username: string, password?: string): MessagePayload {
    let existingPlayer = this.players.find(p => p.username === username);

    if (!existingPlayer) {
      existingPlayer = {
        id: this.nextId++,   // ✅ auto-increment id
        username,
        password,         // optional
        score: 0          // initialize score
      };

      this.players = [...this.players, existingPlayer]; // Update local state immutably
    }
    this.currentUser = existingPlayer;
    this.wsService.joinLobby(username);    // Notify backend lobby join

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

  setCurrentUser(user: MessagePayload) {
    this.currentUser = user;
    localStorage.setItem('currentUser', JSON.stringify(user)); // optional persistence
  }

  getCurrentUser(): MessagePayload | null {
    if (!this.currentUser) {
      const stored = localStorage.getItem('currentUser');
      if (stored) {
        this.currentUser = JSON.parse(stored);
      }
    }
    return this.currentUser;
  }

  clearCurrentUser() {
    this.currentUser = null;
    localStorage.removeItem('currentUser');
  }

  setPlayersFromServer(players: MessagePayload[]) {
    this.players = players;
  }
}
