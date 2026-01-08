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

  addOrGetPlayer(username: string, mobile_number: string): MessagePayload {
  let existingPlayer = this.players.find(p => p.username === username);

  if (!existingPlayer) {
    // ✅ Do not assign id here — wait for backend
    existingPlayer = {
      id: this.nextId++, // temporary placeholder until backend assigns
      username: username,
      mobile_number: mobile_number,
      points: 0
    };

    console.log(existingPlayer);
    this.players = [...this.players, existingPlayer];
  }

  this.currentUser = existingPlayer;

  // ✅ Notify backend to join lobby, backend will respond with PlayerDTO including correct id
  // this.wsService.joinLobby(username);

  return existingPlayer;
}


  updateScore(username: string, points: number) {
    const player = this.players.find(p => p.username === username);
    if (player) {
      player.points = (player.points ?? 0) + points;

      // Sync score update with backend
      this.wsService.sendMessage({
        type: 'UPDATE_SCORE',
        payload: { username, points: player.points }
      });
    }
  }

  getLeaderboard(): MessagePayload[] {
  return [...this.players] // clone
    .sort((a, b) => (b.points ?? 0) - (a.points ?? 0))
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
