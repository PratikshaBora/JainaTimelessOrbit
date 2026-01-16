import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import * as confetti from 'canvas-confetti';

@Component({
  selector: 'app-winner',
  templateUrl: './winner.page.html',
  styleUrls: ['./winner.page.scss'],
  standalone:false
})
export class WinnerPage implements OnInit {
  scores: any[] = [];
  winner: any;
  endRoomTimer: any;

  constructor(private router: Router) {}

  ngOnInit() {
    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras.state as { scores: any[] };

    if (state?.scores) {
      this.scores = state.scores;
      this.winner = this.scores[0];
    }

    // ⏳ Auto-end room after 3 minutes
    this.endRoomTimer = setTimeout(() => {
      this.endRoom();
    }, 3 * 60 * 1000); // 3 minutes
  }

  leaveRoom() {
    clearTimeout(this.endRoomTimer); // stop auto-end for this user
    this.router.navigate(['/lobby'], { state: { fromWinner: true } });
  }

  endRoom() {
    clearTimeout(this.endRoomTimer);
    // Optionally notify backend to close room for all players
    // this.wsService.endRoom(this.roomId);

    this.router.navigate(['/lobby'], { state: { fromWinner: true } });
  }
}
