import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { WebsocketService } from '../services/websocket.service';
import { MessagePayload } from '../models/message-payload';
import { PlayerService } from '../services/player.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
  standalone: false,
})
export class HomePage implements OnInit {
  leaderboard: MessagePayload[] = [];
  currentPosition: number | string = 'Not Started';
  private subscription!: Subscription;

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
      console.log(currentUser);
      if (currentUser) {
        const index = this.leaderboard.findIndex(p => p.username === currentUser.username);
        this.currentPosition = index >= 0 ? index + 1 : 'Not in Top 5';
      }
    });
  }
  startGame() {
    console.log("inside home start game");
    this.currentPosition = 'Waiting in Lobby';
    this.router.navigate(['/lobby']);
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }
}
