import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { WebsocketService } from '../services/websocket';

interface Card {
  aara?: string;
  dwar?: string;
  type?: string;
  points?: number;
}

interface Player {
  id: number;
  username: string;
  score: number;
  handCount: number;
  isTurn?: boolean;
}

@Component({
  selector: 'app-room',
  templateUrl: './room.page.html',
  styleUrls: ['./room.page.scss'],
  standalone: false,
})
export class RoomPage implements OnInit, OnDestroy {
  players: Player[] = [];
  activePlayer?: Player;
  isMyTurn = false;

  drawPileCount = 0;
  discardPileCount = 0;
  topDiscardCard?: Card;

  myHand: Card[] = [];
  selectedCard?: Card;

  turnTimer = 60;
  jjTimer = 60;
  private turnInterval: any;
  private jjInterval: any;

  roomId: number = 0; // numeric roomId
  myPlayerId: number = 1; // set dynamically after join

  constructor(private router: Router, private wsService: WebsocketService) {}

  ionViewDidEnter()
  {
    this.ngOnInit();
  }

  ngOnInit() {
    this.wsService.connect();

    // Subscribe to game updates
    this.wsService['stompClient'].subscribe('/topic/game', (message: any) => {
      const state = JSON.parse(message.body);
      this.applyState(state);
    });

    // Notify backend that this player entered the lobby
    console.log('ngOnInit');
    this.wsService.joinLobby('Pratiksha');
  }

  ngOnDestroy() {
    clearInterval(this.turnInterval);
    clearInterval(this.jjInterval);
    this.wsService.disconnect();
  }

  private applyState(state: any): void {
    this.players = state.players;
    this.activePlayer = state.activePlayer;
    this.isMyTurn = state.isMyTurn;
    this.myHand = state.myHand;
    this.drawPileCount = state.drawPileCount;
    this.discardPileCount = state.discardPileCount;
    this.topDiscardCard = state.topDiscardCard;

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
