import { WinnerPage } from './../winner/winner.page';
import { Component, OnDestroy, OnInit, Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { WebsocketService } from '../services/websocket.service';
import { PlayerService } from '../services/player.service';
import { Card, PlayerDTO, GameRoomDTO } from '../models/game.models';
import { StompSubscription } from '@stomp/stompjs';
import { MessagePayload } from '../models/message-payload';
import { Observable, Subscription } from 'rxjs';
import { AlertController, ToastController } from '@ionic/angular';

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
@Injectable({providedIn:'root'})
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
  private roomSub?: Subscription;
  roomData!: GameRoomDTO;
  isRoomLoaded = false;
  currentAara ='';
  winner ?: PlayerDTO;

  isDrawing = false;
  cardJustPlayed = false;

  private hasSaidJJ = false;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private wsService: WebsocketService,
    private playerService: PlayerService,
    private alertCtrl: AlertController,
    private toastCtrl: ToastController
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
      // ✅ Apply snapshot immediately
      this.roomData = state.room;
      this.roomId = state.room.roomId;
      localStorage.setItem('roomId', String(this.roomId));   // ✅ save
      this.isRoomLoaded = true;
      this.winner = state.room.winner;
      this.applyState(state.room);
    } else {
      // ✅ Fallback: get roomId from route params
      const idParam = this.route.snapshot.paramMap.get('id');
      if (!idParam) {
        console.error('No roomId found in route params');
        this.router.navigate(['/home']);
        return;
      }
      this.roomId = +idParam;
    }
    // ✅ Always subscribe to room updates
    console.log("Current Room ID : ",this.roomId);
    this.wsService.subscribeToGameRoom(this.roomId);
    this.roomSub = this.wsService.getGameRoomUpdates().subscribe(room => {
      if (room) {
        this.roomData = room;
        this.isRoomLoaded = true;
        this.applyState(room);
      }
    });
    // ✅ Listen for scoreboard updates
    this.wsService.scoreboard$.subscribe(scores => {
      if (scores && scores.length > 0) {
        // Sort by points ascending (lowest = winner)
        let sorted = [...scores].sort((a, b) => a.points - b.points);

        // Assign ranks (with tie handling)
        let rank = 1;
        let lastPoints: number | null = null;
        sorted = sorted.map((player, index) => {
          if (player.points !== lastPoints) {
            rank = index + 1;
          }
          lastPoints = player.points;
          return { ...player, rank };
        });

        console.log("Final scoreboard with ranks:", sorted);

        // Navigate to scoreboard page with data
        this.router.navigate(['/winner'], { state: { scores: sorted } });
      }
    });

    this.wsService.messageSubject$.subscribe(msg => {
      if(msg){
        this.showJaiJinendraAlert(msg);
      }
    })
  }

  async showJaiJinendraAlert(message: string) {
    const alert = await this.alertCtrl.create({
      header: '🙏 Jai Jinendra!',
      message: message, // e.g. "PlayerX has declared Jai Jinendra!"
      buttons: ['OK']
    });
    await alert.present();
  }

  ngOnDestroy() {
    clearInterval(this.turnInterval);
    clearInterval(this.jjInterval);
    this.roomSub?.unsubscribe();
  }

  private applyState(state: GameRoomDTO): void {
    this.roomData = state;

    if(state.winner){
      this.winner = state.winner;
      this.stopGameAndCelebrate();
    }

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
    // --- UPDATED HAND LOGIC ---
    const rawHand = this.myPlayer?.hand ?? [];
    this.currentAara = state.currentAara;
    console.log("Top card from discard pile : ",this.topDiscardCard);

    this.isMyTurn = state.currentPlayerId === this.myPlayerId;
    // Calculate playability for each card based on the current Aara
    this.myHand = rawHand.map(card => {
      return {
        ...card,
        isPlayable: this.checkIfCardIsPlayable(card, state)
      };
    });
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
  async playCard(card: Card): Promise<void> {
    console.log("inside play card method : ",card," play of : ",this.currentUserName);
    if(card.type === "WILD" && card.dwar === "COLOR_CHANGE_ADD4" || card.dwar === "COLOR_CHANGE"){
        const alert = await this.alertCtrl.create({
          header: 'Choose Aara',
          message: 'Select the color you want to change to:',
          buttons: [
            {
              text: 'FIRST - BROWN',
              handler: () => this.confirmAara(card, 'FIRST')
            },
            {
              text: 'SECOND - GREEN',
              handler: () => this.confirmAara(card, 'SECOND')
            },
            {
              text: 'THIRD - BLUE',
              handler: () => this.confirmAara(card, 'THIRD')
            },
            {
              text: 'FOURTH - PINK',
              handler: () => this.confirmAara(card, 'FOURTH')
            },
            {
              text: 'FIFTH - PURPLE',
              handler: () => this.confirmAara(card, 'FIFTH')
            },
            {
              text: 'SIXTH - MUD_GREEN',
              handler: () => this.confirmAara(card, 'SIXTH')
            }
          ]
        });
        await alert.present();
        return; // stop here until user chooses
      }
    if (!this.isMyTurn || !card) return;
    this.cardJustPlayed = true; // Trigger discard animation
    this.wsService.playCard(this.roomId, this.myPlayerId, card);
    // Reset flag so next card can animate too
    setTimeout(() => { this.cardJustPlayed = false; }, 600);

    if (this.selectedCard === card) this.selectedCard = undefined;
  }
  private confirmAara(card: Card, chosenAara: string) {
    console.log('Chosen aara:', chosenAara);
    // attach chosen aara to card payload
    card.newAara = chosenAara;
    this.wsService.playCard(this.roomId, this.myPlayerId, card);
  }

  /**
   * Logic to determine if a card should glow based on current Aara and Discard pile
   */
  private checkIfCardIsPlayable(card: Card, state: GameRoomDTO): boolean {
    if (!this.isMyTurn) return false;
    if (!this.topDiscardCard) return true; // Can play anything if pile is empty

    const topCard = this.topDiscardCard;
    const currentRoomAara = state.currentAara.toUpperCase();

    const AARA_COLOR_MAP: { [key: string]: string } = {
      'FIRST': '#8d6e63',  // Brown
      'SECOND': '#4caf50', // Green
      'THIRD': '#2196f3',  // Blue
      'FOURTH': '#f06292', // Pink
      'FIFTH': '#9c27b0',  // Purple
      'SIXTH': '#546e7a'   // Mud Green
    };

    const currentAaraColor = AARA_COLOR_MAP[currentRoomAara];

    if (card.type === 'WILD') return true;
    const matchesAara = card.aara.toUpperCase() === currentRoomAara;
    const matchesDwar = card.dwar === topCard.dwar;
    return matchesAara || matchesDwar;
  }
  // Update drawCard method
  drawCard(): void {
    if (!this.isMyTurn || this.drawPileCount === 0) return;

    this.isDrawing = true; // Start animation
    this.wsService.drawCard(this.roomId, this.myPlayerId);

    // Hide the ghost card after animation finishes
    setTimeout(() => { this.isDrawing = false; }, 600);
  }

  jaiJinendra(): void {
    // GUARD: Stop if it's NOT my turn OR if I have too many cards
    if (!this.isMyTurn || this.myHand.length > 1) {
      console.log("Cannot say Jai Jinendra right now.");
      return;
    }
    this.hasSaidJJ = true;
    clearInterval(this.jjInterval);
    clearInterval(this.turnInterval);
    this.wsService.jaiJinendra(this.roomId, this.myPlayerId);
  }

  private stopGameAndCelebrate(): void
  {
    // Stop timers
    clearInterval(this.turnInterval);
    clearInterval(this.jjInterval);
    // Optionally unsubscribe from further updates
    this.roomSub?.unsubscribe();
    // Trigger fireworks or celebration
    this.showFireworks();
  }
  showFireworks()
  {
    // Or show an Ionic alert
    this.alertCtrl.create({
      header: '🎉 Game Over!',
      message: `Winner is ${this.winner?.username}!`,
      buttons: ['OK']
    }).then(alert => alert.present());
  }

  leaveRoom(): void {
    this.wsService.disconnect();
    this.router.navigate(['/home']);
  }

  // --- Timers ---
  private startTurnTimer(): void {
    clearInterval(this.turnInterval);
    this.turnTimer = 30;
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
    this.jjTimer = 30;
    this.jjInterval = setInterval(() => {
      if(this.hasSaidJJ){    clearInterval(this.jjInterval); return;  }
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
    const safeCount = (count && count > 0) ? Math.floor(count) : 0;
    return new Array(safeCount);
  }
}
