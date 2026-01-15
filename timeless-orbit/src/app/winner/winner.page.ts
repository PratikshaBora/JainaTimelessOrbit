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

  constructor(private router: Router) {}

  ngOnInit() {
    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras.state as { scores: any[] };
    if (state?.scores) {
      this.scores = state.scores;
      this.winner = this.scores[0]; // ✅ first after sorting is winner
    }

    // 🎉 Trigger confetti when page loads
    if (this.winner) {
      confetti({
        particleCount: 200,
        spread: 70,
        origin: { y: 0.6 },
        colors: ['#fff8e7', '#fbe4c4', '#ffcc80', '#d2691e', '#ffd700'] // cream, soft orange, saffron, brown, gold
      });
    }
  }

  getRank(playerId: number): number {
    return this.scores.findIndex(p => p.id === playerId) + 1;
  }
}
