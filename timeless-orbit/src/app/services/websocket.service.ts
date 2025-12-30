import { Injectable } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import * as SockJS from 'sockjs-client';
import { BehaviorSubject } from 'rxjs';
import { MessagePayload } from '../models/message-payload';

@Injectable({
  providedIn: 'root',
})
export class WebsocketService {

  private stompClient!: Client;

  // Subjects for reactive state
  private playersSubject = new BehaviorSubject<MessagePayload[]>([]);
  players$ = this.playersSubject.asObservable();

  private scoreboardSubject = new BehaviorSubject<any[]>([]);
  scoreboard$ = this.scoreboardSubject.asObservable();

  private roomsSubject = new BehaviorSubject<any[]>([]);
  rooms$ = this.roomsSubject.asObservable();

  constructor() {}

  // --- Connection setup ---
  connect(callback: Function | null = null) {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      reconnectDelay: 5000,
      debug: (msg) => console.log(msg),
    });

    this.stompClient.onConnect = () => {
      console.log('Connected to WebSocket');

      // ✅ Subscribe to lobby updates (always a list of players)
      this.stompClient.subscribe('/topic/lobby', (message: IMessage) => {
        const backendPlayers = JSON.parse(message.body) as any[];

        const mappedPlayers: MessagePayload[] = backendPlayers.map(p => ({
          id:Number(p.id),
          username: p.username,
          score: p.points,
          roomId: p.roomId,
          // playerId: p.id,
        }));
        this.playersSubject.next(mappedPlayers);
      });

      // ✅ Subscribe to game updates
      this.stompClient.subscribe('/topic/game', (message: IMessage) => {
        const update = JSON.parse(message.body);
        console.log('Game update:', update);
      });

      // ✅ Subscribe to scoreboard updates
      this.stompClient.subscribe('/topic/scoreboard', (message: IMessage) => {
        const scores = JSON.parse(message.body);
        console.log('Scoreboard update:', scores);
        this.scoreboardSubject.next(scores);
      });

      // ✅ Subscribe to rooms
      this.stompClient.subscribe('/topic/rooms', (message: IMessage) => {
        const room = JSON.parse(message.body);
        console.log('Room created:', room);
        this.roomsSubject.next([...this.roomsSubject.value, room]);
      });

      // ✅ run callback only after connection is established
      if (callback) {
        callback();
      }
    };
    this.stompClient.activate();
  }

  disconnect() {
    if (this.stompClient) {
      this.stompClient.deactivate();
    }
  }

  // --- Lobby actions ---
  joinLobby(username: string) {
    console.log('joinLobby',username,this.stompClient?.connected);
    if (this.stompClient?.connected) {
      this.stompClient.publish({
        destination: '/app/join', // ✅ matches @MessageMapping("/join")
        body: JSON.stringify({ username }),
      });
    }
  }

  leaveLobby(playerId: number) {
    if (this.stompClient?.connected) {
      this.stompClient.publish({
        destination: '/app/leaveLobby',
        body: JSON.stringify({ id: playerId }),
      });
    }
  }

  requestLobbyStatus() {
    if (this.stompClient?.connected) {
      this.stompClient.publish({
        destination: '/app/lobbyStatus',
        body: '{}',
      });
    }
  }

  // --- Game actions ---
  playCard(roomId: number, playerId: number, card: any) {
    this.publish('/app/playCard', { roomId, playerId, card });
  }

  drawCard(roomId: number, playerId: number) {
    this.publish('/app/drawCard', { roomId, playerId });
  }

  pickDiscard(roomId: number, playerId: number) {
    this.publish('/app/pickDiscard', { roomId, playerId });
  }

  jaiJinendra(roomId: number, playerId: number) {
    this.publish('/app/jaiJinendra', { roomId, playerId });
  }

  autoPenaltyDraw(roomId: number, playerId: number) {
    this.publish('/app/penaltyDraw', { roomId, playerId, reason: 'timeout' });
  }

  jjTimeoutPenalty(roomId: number, playerId: number) {
    this.publish('/app/jjPenalty', { roomId, playerId, reason: 'jjTimeout' });
  }

  endgameResolution(roomId: number) {
    this.publish('/app/endgame', { roomId });
  }

  drawTwo(roomId: number, playerId: number) {
    this.publish('/app/drawTwo', { roomId, playerId });
  }

  drawFour(roomId: number, playerId: number) {
    this.publish('/app/drawFour', { roomId, playerId });
  }

  startGame() {
    if (this.stompClient?.connected) {
      this.stompClient.publish({
        destination: '/app/startGame',
        body: ''   // ✅ no need to send players list
      });
    }
  }

  sendMessage(message: { type: string; payload: any }) {
    if (this.stompClient?.connected) {
      this.stompClient.publish({
        destination: '/app/' + message.type.toLowerCase(), // e.g. /app/update_score
        body: JSON.stringify(message.payload),
      });
    }
  }
  // --- Helper publish method ---
  private publish(destination: string, body: any) {
    if (this.stompClient?.connected) {
      this.stompClient.publish({
        destination,
        body: JSON.stringify(body),
      });
    }
  }

  subscribeToGame(callback: (state: any) => void) {
  this.stompClient.subscribe('/topic/game', (message: any) => {
    callback(JSON.parse(message.body));
  });
}
}
