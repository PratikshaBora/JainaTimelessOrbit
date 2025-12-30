import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { WebsocketService } from '../services/websocket.service';
import { MessagePayload } from '../models/message-payload';
import { PlayerService } from '../services/player.service';

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
  rooms$ = this.wsService.rooms$;

  constructor(
    private router: Router,
    private wsService: WebsocketService,
    private playerService : PlayerService
  ) {}

  currentUserName: string = '';

  ngOnInit() {
    const currentUser = this.playerService.getCurrentUser();
    console.log(currentUser);
    console.log("username : "+currentUser?.username);

    if (!currentUser) {
      console.warn('No current user found, redirecting to login...');
      this.router.navigate(['/home']);
      return;
    }

    this.wsService.connect(
      () => {
        console.log('joining lobby');
        this.currentUserName = currentUser.username;   // ✅ store name
        this.wsService.joinLobby(currentUser.username); // ✅ use real username
      }
    );

    this.wsService.players$.subscribe((players: MessagePayload[]) => {
      this.players = players;
    });
    // this.interval = setInterval(() => {
    //   this.timeLeft--;
    //   if (this.timeLeft <= 0) {
    //     clearInterval(this.interval);
    //     this.createRoom();
    //   }
    // }, 1000);
  }

  leaveLobby() {
    clearInterval(this.interval);
    this.wsService.disconnect();
    this.router.navigate(['/home']);
  }

  createRoom() {
    console.log('Requesting backend to start game...');
    this.wsService.startGame();   // ✅ no players array
    this.router.navigate(['/room']);
  }

  playRoom() {

  }
  refreshLobby() {
    this.wsService.requestLobbyStatus();
  }

  ngOnDestroy() {
    clearInterval(this.interval);
    this.wsService.disconnect();
  }
}
