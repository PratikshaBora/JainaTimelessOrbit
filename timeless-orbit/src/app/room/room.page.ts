import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { WebsocketService } from '../services/websocket.service';
import { PlayerService } from '../services/player.service';
import { Card, PlayerDTO, GameRoomDTO } from '../models/game.models';
import { StompSubscription } from '@stomp/stompjs';
import { MessagePayload } from '../models/message-payload';

type OpponentSlots = {
  top?: PlayerDTO & { handCount: number; isTurn: boolean };
  right?: PlayerDTO & { handCount: number; isTurn: boolean };
  left?: PlayerDTO & { handCount: number; isTurn: boolean };
};

@Component({
  selector: 'app-room',
  templateUrl: './room.page.html',
  styleUrls: ['./room.page.scss'],
  standalone: false
})
export class RoomPage implements OnInit, OnDestroy {
  players: (PlayerDTO & { handCount: number; isTurn: boolean; hand?: Card[] })[] = [];
  opponentsBySlot: OpponentSlots = {};
  activePlayer?: PlayerDTO;
  isMyTurn = false;

  currentUser: MessagePayload | null = null;
  currentUserName = '';
  myPlayer?: PlayerDTO;
  myPlayerId = 0;
  myHand: Card[] = [];
  selectedCard?: Card;

  drawPile: Card[] = [];
  discardPile: Card[] = [];
  topDiscardCard?: Card;
  drawPileCount = 0;
  discardPileCount = 0;

  timeLeft = 60;
  turnTimer = 60;
  jjTimer = 60;
  private turnInterval: any;
  private jjInterval: any;

  roomId!: number;
  private roomSub?: StompSubscription;
  roomData!: GameRoomDTO;
  isRoomLoaded = false;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private wsService: WebsocketService,
    private playerService: PlayerService
  ) {}

  ngOnInit() {
    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras.state as { room: GameRoomDTO; username?: string };

    this.currentUser = this.playerService.getCurrentUser();
    if (this.currentUser) {
      this.myPlayerId = this.currentUser.id;
      this.currentUserName = this.currentUser.username;
    } else {
      this.router.navigate(['/home']);
      return;
    }

    if (state?.room) {
      this.roomData = state.room;
      this.roomId = state.room.roomId;
      this.isRoomLoaded = true;
      this.applyState(state.room);
    } else {
      this.route.paramMap.subscribe(params => {
        const roomId = Number(params.get('id'));
        this.roomId = roomId;
        this.roomSub = this.wsService.subscribeToRoom((room: GameRoomDTO) => {
          this.roomData = room;
          this.isRoomLoaded = true;
          this.applyState(room);
        });
      });
    }
  }

  ngOnDestroy() {
    clearInterval(this.turnInterval);
    clearInterval(this.jjInterval);
    this.roomSub?.unsubscribe();
  }

  private applyState(state: GameRoomDTO): void {
    this.roomData = state;

    this.players = state.players.map(p => {
      const isMe = p.id === this.myPlayerId || p.username === this.currentUserName;
      const handCount = p.handCount ?? (p.hand?.length ?? 0);
      return {
        ...p,
        hand: isMe ? (p.hand ?? []) : this.createCountArray(handCount).map(() => ({ back: true } as any)),
        handCount,
        isTurn: p.id === state.currentPlayerId,
      };
    });

    this.myPlayer = state.players.find(p => p.id === this.myPlayerId || p.username === this.currentUserName);
    this.myHand = this.myPlayer?.hand ?? [];

    this.activePlayer = state.players.find(p => p.id === state.currentPlayerId);

    this.drawPile = state.drawPile ?? [];
    this.discardPile = state.discardPile ?? [];
    this.drawPileCount = this.drawPile.length;
    this.discardPileCount = this.discardPile.length;
    this.topDiscardCard = this.discardPile[this.discardPileCount - 1];

    this.isMyTurn = state.currentPlayerId === this.myPlayerId;
    if (state.turnTimeLeft !== undefined) this.timeLeft = state.turnTimeLeft;

    if (this.isMyTurn) {
      this.startTurnTimer();
      if (this.myHand.length === 1) this.startJaiJinendraTimer();
    } else {
      clearInterval(this.turnInterval);
      clearInterval(this.jjInterval);
    }

    this.mapOpponentsToSlots(state);
  }

  private mapOpponentsToSlots(state: GameRoomDTO): void {
    const players = state.players ?? [];
    if (!players.length || !this.currentUserName) {
      this.opponentsBySlot = {};
      return;
    }

    const youIndex = players.findIndex(p => p.username === this.currentUserName || p.id === this.myPlayerId);
    if (youIndex < 0) {
      this.opponentsBySlot = {};
      return;
    }

    const total = players.length;
    const getRel = (offset: number) => players[(youIndex + offset + total) % total];

    const right = getRel(1);
    const top = getRel(2);
    const left = getRel(3);

    const toSlot = (p?: PlayerDTO) => {
      if (!p) return undefined;
      if (p.username === this.currentUserName || p.id === this.myPlayerId) return undefined;
      const enriched = this.players.find(x => x.id === p.id) ?? {
        ...p,
        handCount: p.handCount ?? (p.hand?.length ?? 0),
        isTurn: p.id === state.currentPlayerId
      };
      return enriched;
    };

    const others = players.filter(p => p.username !== this.currentUserName && p.id !== this.myPlayerId);
    if (others.length === 1) {
      this.opponentsBySlot = { top: toSlot(others[0]) };
      return;
    }
    if (others.length === 2) {
      this.opponentsBySlot = { top: toSlot(top), right: toSlot(right) };
      return;
    }

    this.opponentsBySlot = {
      top: toSlot(top),
      right: toSlot(right),
      left: toSlot(left),
    };
  }

  // --- Actions ---
  playCard(card?: Card): void {
    if (!this.isMyTurn || !card) return;
    this.wsService.playCard(this.roomId, this.myPlayerId, card);
    if (this.selectedCard === card) this.selectedCard = undefined;
  }

  drawCard(): void {
    if (!this.isMyTurn || this.drawPileCount === 0) return;
    this.wsService.drawCard(this.roomId, this.myPlayerId);
  }

  pickDiscard(): void {
    if (!this.isMyTurn || !this.topDiscardCard) return;
    this.wsService.pickDiscard(this.roomId, this.myPlayerId);
  }

  jaiJinendra(): void {
    if (!this.isMyTurn || this.myHand.length !== 1) return;
    this.wsService.jaiJinendra(this.roomId, this.myPlayerId);
  }

  leaveRoom(): void {
    this.wsService.disconnect();
    this.router.navigate(['/home']);
  }

  // --- Timers ---
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

  // --- Template helpers ---
  trackByPlayerId(index: number, player: PlayerDTO): number {
    return player.id;
  }

  trackByIndex(index: number): number {
    return index;
  }

  trackByCard(index: number, card: Card): number {
    return (card as any)?.id ?? index;
  }

  createCountArray(count: number): any[] {
    const safe = Math.max(0, count || 0);
    return Array.from({ length: safe });
  }
}
