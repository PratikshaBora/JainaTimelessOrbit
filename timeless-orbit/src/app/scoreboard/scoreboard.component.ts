import { Component, OnInit } from '@angular/core';
import { WebsocketService } from '../services/websocket.service';
import { IonCard, IonGrid, IonRow, IonCardContent, IonCardHeader, IonCardTitle } from "@ionic/angular/standalone";

@Component({
  selector: 'app-scoreboard',
  templateUrl: './scoreboard.component.html',
  styleUrls: ['./scoreboard.component.scss'],
  standalone:false
})
export class ScoreboardComponent  implements OnInit {
  scores: any[] = [];
  constructor(private wsService:WebsocketService) { }

  ngOnInit() {
    // subscribe to scoreboard updates
    this.wsService.scoreboard$.subscribe((data) => {
      this.scores = data;
    });
  }

}
