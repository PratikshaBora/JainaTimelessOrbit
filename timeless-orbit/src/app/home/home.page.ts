import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { WebsocketService } from '../services/websocket.service';
import { MessagePayload } from '../models/message-payload';
import { PlayerService } from '../services/player.service';

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
  standalone: false,
})
export class HomePage implements OnInit {
  leaderboard: MessagePayload[] = [];
  currentPosition: number | string = 'Not Started';

  constructor(
    private router: Router,
    private wsService: WebsocketService,
    private playerService: PlayerService
  ) {}

  ngOnInit(): void {
    this.wsService.players$.subscribe((players: MessagePayload[]) => {
      this.leaderboard = players
        .sort((a, b) => (b.score ?? 0) - (a.score ?? 0))
        .slice(0, 5);

      const currentUser = this.playerService.getCurrentUser();
      if (currentUser) {
        this.currentPosition =
          this.leaderboard.findIndex(p => p.username === currentUser.username) + 1;
      }
    });
  }
  startGame() {
    this.currentPosition = 'Waiting in Lobby';
    this.router.navigate(['/lobby']);
  }
}
