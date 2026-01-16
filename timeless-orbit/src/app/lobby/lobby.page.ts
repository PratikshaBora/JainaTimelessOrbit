import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { WebsocketService } from '../services/websocket.service';
import { PlayerService } from '../services/player.service';
import { MessagePayload } from '../models/message-payload';
import { GameRoomDTO } from '../models/game.models';

@Component({
  selector: 'app-lobby',
  templateUrl: './lobby.page.html',
  styleUrls: ['./lobby.page.scss'],
  standalone: false
})
export class LobbyPage implements OnInit {
  playersJoin: MessagePayload[] = [];
  currentUserName = '';

  constructor(
    private wsService: WebsocketService,
    private playerService: PlayerService,
    private router: Router
  ) {}

  ngOnInit() {
    console.log('LobbyPage initialized — subscribing to lobby updates');

    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras.state as { fromWinner?: boolean };

    const currentUser = this.playerService.getCurrentUser();
    if (!currentUser) {
      console.warn('No current user found, redirecting to login...');
      this.router.navigate(['/home']);
      return;
    }
    this.currentUserName = currentUser.username;

    // ✅ Connect and auto‑join lobby
    this.wsService.connect(() => {
      const currentUser = this.playerService.getCurrentUser();
      if (currentUser) {
        this.wsService.joinLobby(currentUser);   // ✅ send full object
      }
    });

    this.wsService.playerJoined$.subscribe((player) => {
      if (!player) return; // skip initial null

      const currentUser = this.playerService.getCurrentUser();
      if (currentUser && player.username === currentUser.username) {
        this.playerService.setCurrentUser(player);
      }
    });

    // ✅ Subscribe to lobby player list
    this.wsService.players$.subscribe((players: MessagePayload[]) => {
      this.playersJoin = players;
      console.log('Lobby players updated:', this.playersJoin);
    });

    this.wsService.rooms$.subscribe((rooms: GameRoomDTO[]) => {
      if (state?.fromWinner) {
        console.log('Returned from winner page — skipping auto-join');
        return;
      }

      const newRoom = rooms[rooms.length - 1];

      // ✅ Only navigate if current user is in that room
      if (newRoom.players.some(p => p.username === this.currentUserName))
      {
        console.log('Auto‑joining room', newRoom.roomId);
        this.router.navigate(['/room', newRoom.roomId], {
          state: { room: newRoom },
        });
      }
    });
  }
}
