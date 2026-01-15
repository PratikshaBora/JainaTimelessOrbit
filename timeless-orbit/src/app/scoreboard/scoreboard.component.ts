import { Component, Input, OnInit } from '@angular/core';
import { WebsocketService } from '../services/websocket.service';
import { IonCard, IonGrid, IonRow, IonCardContent, IonCardHeader, IonCardTitle } from "@ionic/angular/standalone";

@Component({
  selector: 'app-scoreboard',
  templateUrl: './scoreboard.component.html',
  styleUrls: ['./scoreboard.component.scss'],
  standalone:false
})
export class ScoreboardComponent  implements OnInit {

  @Input() scores: any[] = [];   // ✅ external scores
  @Input() title: string = 'Scoreboard'; // ✅ dynamic title
  @Input() showRanks: boolean = false;   // ✅ toggle ranks

  constructor(private wsService:WebsocketService) { }

  ngOnInit() {
    // If no scores passed in, subscribe to live updates (for lobby)
    if (!this.scores || this.scores.length === 0) {
      this.wsService.scoreboard$.subscribe((data) => {
        this.scores = data;
      });
    }
  }

  getRank(playerId: number): number {
    return this.scores.findIndex(p => p.id === playerId) + 1;
  }

}
