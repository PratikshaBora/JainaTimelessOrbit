import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Socket } from 'ngx-socket-io'; // assuming ngx-socket-io
import { WebsocketService } from '../services/websocket';

@Component({
  selector: 'app-lobby',
  templateUrl: './lobby.page.html',
  styleUrls: ['./lobby.page.scss'],
  standalone: false
})
export class LobbyPage implements OnInit {

  players: string[] = [];
  timeLeft: number = 120; // 2 minutes
  interval: any;

  constructor(private router: Router, private wsService: WebsocketService) { }

  ngOnInit() {
    this.wsService.connect(()=>{
      console.log('callback');
      this.wsService.joinLobby('Pratiksha');
    });
    console.log('ngOnInit');

    // Subscribe to player updates
    this.wsService.players$.subscribe(players => {
      this.players = players;
    });

    // Start countdown
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
