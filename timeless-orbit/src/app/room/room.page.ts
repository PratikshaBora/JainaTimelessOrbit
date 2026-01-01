import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { WebsocketService } from '../services/websocket.service';
import { PlayerService } from '../services/player.service';
import { Card, PlayerDTO, GameRoomDTO } from '../models/game.models'; // ✅ use shared models

@Component({
  selector: 'app-room',
  templateUrl: './room.page.html',
  styleUrls: ['./room.page.scss'],
  standalone: false,
})
export class RoomPage implements OnInit, OnDestroy {
  players: (PlayerDTO & { handCount: number; isTurn: boolean })[] = [];
  activePlayer?: PlayerDTO;
  isMyTurn = false;

  topDiscardCard?: Card;
  myHand: Card[] = [];
  selectedCard?: Card;

  drawPileCount = 0;
  discardPileCount = 0;

  turnTimer = 60;
  jjTimer = 60;
  private turnInterval: any;
  private jjInterval: any;

  roomId: number = 0; // numeric roomId
  myPlayerId: number = 0; // will be set from PlayerService

  constructor(
    private router: Router,
    private wsService: WebsocketService,
    private playerService: PlayerService
  ) {}

  // ionViewDidEnter()
  // {
  //   this.ngOnInit();
  // }

  ngOnInit() {
    // ✅ Ensure connection exists
    this.wsService.connect();

    // ✅ Subscribe via WebsocketService helper, not raw stompClient
    this.wsService.subscribeToGame((state: any) => {
      this.applyState(state);
    });

    // ✅ Use actual logged-in user
    const currentUser = this.playerService.getCurrentUser();
    if (currentUser) {
      this.myPlayerId = currentUser.id;
      this.wsService.joinLobby(currentUser.username);
    }
  }

  ngOnDestroy() {
    clearInterval(this.turnInterval);
    clearInterval(this.jjInterval);
    this.wsService.disconnect();
  }

  private applyState(state: GameRoomDTO): void {
  this.players = state.players.map(p => ({
    ...p,
    handCount: p.hand?.length ?? 0,
    isTurn: p.id === state.currentPlayerId
  }));

  this.activePlayer = state.players.find(p => p.id === state.currentPlayerId);
  this.isMyTurn = state.currentPlayerId === this.myPlayerId;
  this.myHand = state.players.find(p => p.id === this.myPlayerId)?.hand ?? [];

  this.drawPileCount = state.drawPile.length;
  this.discardPileCount = state.discardPile.length;
  this.topDiscardCard = state.discardPile[state.discardPile.length - 1];

  if (this.isMyTurn) {
    this.startTurnTimer();
    if (this.myHand.length === 1) this.startJaiJinendraTimer();
  } else {
    clearInterval(this.turnInterval);
    clearInterval(this.jjInterval);
  }

  if (this.drawPileCount === 0) {
    this.wsService.endgameResolution(this.roomId);
  }
}

  // --- UI actions ---
  onSelectCard(card: Card): void {
    if (!this.isMyTurn) return;
    this.selectedCard = card;
  }

  onPlaySelected(): void {
    if (!this.isMyTurn || !this.selectedCard) return;
    this.wsService.playCard(this.roomId, this.myPlayerId, this.selectedCard);
    this.selectedCard = undefined;
  }

  onDrawCard(): void {
    if (!this.isMyTurn || this.drawPileCount === 0) return;
    this.wsService.drawCard(this.roomId, this.myPlayerId);
  }

  onPickDiscard(): void {
    if (!this.isMyTurn) return;
    this.wsService.pickDiscard(this.roomId, this.myPlayerId);
  }

  onJaiJinendra(): void {
    if (!this.isMyTurn || this.myHand.length !== 1) return;
    this.wsService.jaiJinendra(this.roomId, this.myPlayerId);
  }

  leaveRoom(): void {
    this.wsService.disconnect();
    this.router.navigate(['/home']);
  }

  private startTurnTimer(): void {
    clearInterval(this.turnInterval);
    this.turnTimer = 60;
    this.turnInterval = setInterval(() => {
      this.turnTimer--;
      if (this.turnTimer <= 0) {
        clearInterval(this.turnInterval);
        this.wsService.autoPenaltyDraw(this.roomId, this.myPlayerId);
      }
    }, 1000);
  }

  private startJaiJinendraTimer(): void {
    clearInterval(this.jjInterval);
    if (this.myHand.length !== 1) return;
    this.jjTimer = 60;
    this.jjInterval = setInterval(() => {
      this.jjTimer--;
      if (this.jjTimer <= 0) {
        clearInterval(this.jjInterval);
        this.wsService.jjTimeoutPenalty(this.roomId, this.myPlayerId);
      }
    }, 1000);
  }
}
