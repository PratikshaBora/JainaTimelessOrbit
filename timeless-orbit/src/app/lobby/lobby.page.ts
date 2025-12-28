import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { WebsocketService } from '../services/websocket.service';
import { MessagePayload } from '../models/message-payload';

@Component({
  selector: 'app-lobby',
  templateUrl: './lobby.page.html',
  styleUrls: ['./lobby.page.scss'],
  standalone: false
})
export class LobbyPage implements OnInit, OnDestroy {

  players: MessagePayload[] = [];
  timeLeft: number = 120;
  interval: any;

  constructor(
    private router: Router,
    private wsService: WebsocketService
  ) {}

  ngOnInit() {
    this.wsService.connect();
    this.wsService.joinLobby('Pratiksha');

    this.wsService.players$.subscribe((players: MessagePayload[]) => {
      this.players = players;
    });

    this.interval = setInterval(() => {
      this.timeLeft--;
      if (this.timeLeft <= 0) {
        clearInterval(this.interval);
        this.createRoom();
      }
    }, 1000);
  }

  leaveLobby() {
    clearInterval(this.interval);
    this.wsService.disconnect();
    this.router.navigate(['/home']);
  }

  createRoom() {
    this.wsService.createRoom(this.players);
    this.router.navigate(['/room']);
  }

  ngOnDestroy() {
    clearInterval(this.interval);
    this.wsService.disconnect();
  }
}
