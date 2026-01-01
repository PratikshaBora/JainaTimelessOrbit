import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { WebsocketService } from '../services/websocket.service';
import { MessagePayload } from '../models/message-payload';
import { PlayerService } from '../services/player.service';
import { Card, GameRoomDTO, PlayerDTO } from '../models/game.models';

@Component({
  selector: 'app-lobby',
  templateUrl: './lobby.page.html',
  styleUrls: ['./lobby.page.scss'],
  standalone: false
})
export class LobbyPage implements OnInit, OnDestroy {
  currentRoomId!: number;   // non-null assertion
  players: PlayerDTO[] = [];
  discardPile: Card[] = [];
  drawPile: Card[] = [];
  currentView: string = 'lobby';
  timeLeft: number = 120;
  interval: any;
  rooms$ = this.wsService.rooms$;
  myPlayer?: PlayerDTO;
  playersJoin:MessagePayload[] = [];

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
      this.playersJoin = players;
    });
  }

  selectedCard?: Card;

  selectCard(card: Card) {
    this.selectedCard = card;
  }

  playCard(card: Card) {
    if (!this.currentRoomId) {
      console.error('No room joined yet!');
      return;
    }

    const message = {
      action: 'PLAY_CARD',
      card: card,
      roomId: this.currentRoomId,
      player: this.currentUserName
    };
    this.wsService.sendMessage(message);
    this.selectedCard = undefined;
  }

  // Keyboard shortcut (desktop)
  @HostListener('document:keydown.enter')
  handleEnter() {
    if (this.selectedCard) {
      this.playCard(this.selectedCard);
    }
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

  joinRoom(roomId: number) {
    this.currentRoomId = roomId;
    this.wsService.subscribeToRoom(roomId, (room: GameRoomDTO) =>
    {
      this.players = room.players;
      this.discardPile = room.discardPile;
      this.drawPile = room.drawPile;
      // Identify current player
      this.myPlayer = room.players.find(p => p.username === this.currentUserName);
      this.currentView = 'game'; // switch UI
    });
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
