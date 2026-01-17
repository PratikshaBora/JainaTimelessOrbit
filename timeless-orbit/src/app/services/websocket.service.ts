import { Injectable } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { BehaviorSubject, Observable } from 'rxjs';
import { MessagePayload } from '../models/message-payload';
import { GameRoomDTO, Card } from '../models/game.models';
import * as SockJS from 'sockjs-client';

@Injectable({ providedIn: 'root' })
export class WebsocketService {
  private stompClient!: Client;
  private connected: boolean = false;

  // ✅ Subjects / obeservables
  private playerJoinedSubject = new BehaviorSubject<MessagePayload | null>(null);
  playerJoined$ = this.playerJoinedSubject.asObservable();

  private playersSubject = new BehaviorSubject<MessagePayload[]>([]);
  players$ = this.playersSubject.asObservable();

  private roomsSubject = new BehaviorSubject<any[]>([]);
  rooms$ = this.roomsSubject.asObservable();

  private scoreboardSubject = new BehaviorSubject<any>([]);
  public scoreboard$ = this.scoreboardSubject.asObservable();

  // ✅ Stream of game room updates
  private gameRoomUpdates$ = new BehaviorSubject<GameRoomDTO | null>(null);
  private messageSubject = new BehaviorSubject<string | null>(null);
  public messageSubject$ = this.messageSubject.asObservable();

  private endRoomSubject = new BehaviorSubject<string | null>(null);
  endRoom$ = this.endRoomSubject.asObservable();

  // --- Connection setup ---
  connect(callback: Function | null = null) {
    if (this.connected) {
      if (callback) callback(); // already connected, run callback immediately
      return;
    }

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      reconnectDelay: 5000,
      debug: (msg) => console.log(msg),
    });

    this.stompClient.onConnect = () => {
      console.log('Connected to WebSocket');
      this.connected = true;

      // Lobby + global topics
      this.stompClient.subscribe('/topic/playerJoined', (message) => {
        const player: MessagePayload = JSON.parse(message.body);
        this.playerJoinedSubject.next(player);
      });
      // ✅ Subscribe to lobby updates
      this.stompClient.subscribe('/topic/lobby', (message: IMessage) => {
        const backendPlayers = JSON.parse(message.body) as any[];
        const mappedPlayers: MessagePayload[] = backendPlayers.map(p => ({
          id: Number(p.id),
          username: p.username,
          mobile_number: p.mobile_number,
          points: p.points,
          roomId: p.roomId,
        }));
        this.playersSubject.next(mappedPlayers);
      });

      // ✅ Subscribe to scoreboard updates
      this.stompClient.subscribe('/topic/scoreboard', (message: IMessage) => {
        const scores = JSON.parse(message.body);
        console.log('Scoreboard update:', scores);
        this.scoreboardSubject.next(scores);
      });

      // ✅ Subscribe to rooms (room creation announcements)
      this.stompClient.subscribe('/topic/rooms', (message: IMessage) => {
        const room = JSON.parse(message.body);
        console.log('Room created:', room);
        this.roomsSubject.next([...this.roomsSubject.value, room]);
      });
      // ✅ run callback only after connection is established
      if (callback) {   callback();      }
    };
    this.stompClient.activate();
  }
  // ✅ Room-specific subscription
  subscribeToGameRoom(roomId: number) {
    this.stompClient?.subscribe(`/topic/game/${roomId}`, (message: IMessage) => {
      const update: GameRoomDTO = JSON.parse(message.body);
      console.log(`GameRoom ${roomId} update:`, update);
      this.gameRoomUpdates$.next(update);
    });
  }

  subscibeToJaiJinendra(roomId:number){
    this.stompClient?.subscribe(`/topic/game/${roomId}/jaiJinendra`,(message:IMessage) => {
      const data = message.body;
      console.log(`GameRoom ${roomId} message : `,data);
      this.messageSubject.next(data);
    });
  }
  getGameRoomUpdates(): Observable<GameRoomDTO | null> {
    return this.gameRoomUpdates$.asObservable();
  }

  disconnect() {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.connected = false;
    }
  }
  // --- Generic publish ---
  public sendMessage(message: any, destination: string = '/app/game'): void {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination,
        body: JSON.stringify(message),
      });
    } else {
      console.error('WebSocket not connected, cannot send message');
    }
  }

  // Generic subscribe helper
  subscribe(destination: string, callback: (msg: any) => void): StompSubscription | undefined {
    return this.stompClient?.subscribe(destination, callback);
  }

  // ✅ Room subscription (personalized updates)
  subscribeToRoom(callback: (room: GameRoomDTO) => void) {
    return this.stompClient.subscribe(`/user/queue/room`, message => {
      const room: GameRoomDTO = JSON.parse(message.body);
      callback(room);
    });
  }

  subscribeToEndRoom(roomId: number) {
    this.stompClient?.subscribe(`/topic/game/${roomId}/end`, (message: IMessage) => {
      console.log("End room broadcast:", message.body);
      this.endRoomSubject.next(message.body);
    });
  }

  // --- Lobby actions ---
  joinLobby(player: MessagePayload) {
    console.log('Publishing JOIN_LOBBY for', player);

    if (this.stompClient?.connected) {
      this.stompClient.publish({
        destination: '/app/join',
        body: JSON.stringify(player),   // ✅ send complete currentUser object
      });
    } else {
      console.warn('STOMP not connected yet, cannot publish join');
    }
  }
  rejoinRoom(request: { roomId: number; username: string; mobileNumber: string }) {
    this.stompClient.publish({ destination: '/app/rejoin'});
  }
  requestLobbyStatus() {
    this.stompClient?.publish({ destination: '/app/lobby/status' });
  }

  // --- Game actions ---
  playCard(roomId: number, playerId: number, card: Card) {
    this.stompClient?.publish({
      destination: `/app/game/${roomId}/play`,
      body: JSON.stringify({ roomId, playerId, card }),
    });
  }

  drawCard(roomId: number, playerId: number) {
    this.stompClient?.publish({
      destination: `/app/game/${roomId}/draw`,
      body: JSON.stringify({ roomId, playerId }),
    });
  }

  jaiJinendra(roomId: number, playerId: number) {
    this.stompClient?.publish({
      destination: `/app/game/${roomId}/jaiJinendra`,
      body: JSON.stringify({ roomId,playerId }),
    });
  }

  autoPenaltyDraw(roomId: number, playerId: number) {
    this.stompClient?.publish({
      destination: `/app/game/${roomId}/autoPenaltyDraw`,
      body: JSON.stringify({ roomId,playerId }),
    });
  }

  jjTimeoutPenalty(roomId: number, playerId: number) {
    this.stompClient?.publish({
      destination: `/app/game/${roomId}/jjTimeoutPenalty`,
      body: JSON.stringify({ roomId,playerId }),
    });
  }
  onPlayerJoined(callback: (player: MessagePayload) => void) {
    this.stompClient.subscribe('/topic/playerJoined', message => {
      const player: MessagePayload = JSON.parse(message.body);
      callback(player);
    });
  }
}
