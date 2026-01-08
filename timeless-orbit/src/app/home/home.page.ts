import { Component, OnInit, OnDestroy } from '@angular/core';
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
export class HomePage implements OnInit, OnDestroy {
  leaderboard: MessagePayload[] = [];
  currentPosition: number | string = 'Not Started';
  currentUser!: MessagePayload;   // ✅ consistently use currentUser
  private subscription!: Subscription;

  constructor(
    private router: Router,
    private wsService: WebsocketService,
    private playerService: PlayerService
  ) {}

  ngOnInit(): void {
    // 1. Capture the player object passed from Login Page
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras.state && navigation.extras.state['player']) {
      this.currentUser = navigation.extras.state['player'] as MessagePayload;
      console.log("Fetched player username:", this.currentUser.username);
      console.log("Fetched player mobile number:", this.currentUser.mobile_number);

      // ✅ Store in PlayerService for later use
      this.playerService.setCurrentUser(this.currentUser);
    } else {
      // Fallback: load from PlayerService/localStorage
      const stored = this.playerService.getCurrentUser();
      if (stored) {
        this.currentUser = stored;
        console.log("Fetched player from PlayerService:", this.currentUser);
      }
    }

    // 2. Subscribe to players$ for leaderboard updates
    this.subscription = this.wsService.players$.subscribe((players: MessagePayload[]) => {
      this.leaderboard = players
        .sort((a, b) => (b.points ?? 0) - (a.points ?? 0))
        .slice(0, 5);

      if (this.currentUser) {
        const index = this.leaderboard.findIndex(p => p.username === this.currentUser.username);
        this.currentPosition = index >= 0 ? index + 1 : 'Not in Top 5';
      }
    });
  }

  startGame() {
    console.log("Navigating to lobby with player info...");

    if (!this.currentUser) {
      // Fallback in case the user refreshed the page and state is lost
      this.currentUser = this.playerService.getCurrentUser()!;
    }

    if (!this.currentUser) {
      alert("Player info not found. Please login again.");
      this.router.navigate(['/login']);
      return;
    }

    this.currentPosition = 'Waiting in Lobby';

    // 3. Forward the player information to the Lobby page
    this.router.navigate(['/lobby']);
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }
}
