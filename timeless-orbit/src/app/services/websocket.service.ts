import { Injectable } from '@angular/core';
import { Client } from '@stomp/stompjs';
import * as SockJS from 'sockjs-client';
import { BehaviorSubject } from 'rxjs';
import { MessagePayload } from '../models/message-payload';
import { Card, GameRoomDTO } from '../models/game.models';
import { IMessage } from '@stomp/stompjs';
import {
  PlayCardMessage,
  DrawCardMessage,
  PickDiscardMessage,
  JaiJinendraMessage,
  PenaltyMessage,
  EndgameMessage,
  DrawTwoMessage,
  AaraChangeDrawFourMessage,
  SkipTurnMessage,
  ReverseTurnMessage,
  AaraChangeMessage
} from '../models/game-message';

@Injectable({
  providedIn: 'root',
})
export class WebsocketService {

  private stompClient!: Client;

  // Subjects for reactive state
  private roomSubject = new BehaviorSubject<GameRoomDTO | null>(null);
  room$ = this.roomSubject.asObservable();

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

      // Subscribe to room updates
      this.stompClient.subscribe('/topic/game/${roomId}', (message: IMessage) => {
        const room: GameRoomDTO = JSON.parse(message.body);
        this.roomSubject.next(room);
      });

      // ✅ run callback only after connection is established
      if (callback) {
        callback();
      }
    };
    this.stompClient.activate();
  }

  subscribeToRoom(roomId: number, callback: (room: GameRoomDTO) => void) {
    this.stompClient.subscribe(`/topic/game/${roomId}`, (message: any) => {
      const room: GameRoomDTO = JSON.parse(message.body);
      callback(room);
    });
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
  // --- Helper publish method ---
  private publish(destination: string, body: any) {
    if (this.stompClient?.connected) {
      this.stompClient.publish({
        destination,
        body: JSON.stringify(body),
      });
    }
  }
  // --- Game actions ---
  playCard(roomId: number, playerId: number, card: Card) {
    const payload: PlayCardMessage = { roomId, playerId, card };
    this.publish('/app/playCard', payload);
  }
  drawCard(roomId: number, playerId: number) {
    const payload: DrawCardMessage = { roomId, playerId };
    this.publish('/app/drawCard', payload);
  }
  pickDiscard(roomId: number, playerId: number) {
    const payload: PickDiscardMessage = { roomId, playerId };
    this.publish('/app/pickDiscard', payload);
  }
  jaiJinendra(roomId: number, playerId: number) {
    const payload: JaiJinendraMessage = { roomId, playerId };
    this.publish('/app/jaiJinendra', payload);
  }
  autoPenaltyDraw(roomId: number, playerId: number) {
    const payload: PenaltyMessage = { roomId, playerId, reason: 'timeout' };
    this.publish('/app/penaltyDraw', payload);
  }
  jjTimeoutPenalty(roomId: number, playerId: number) {
    const payload: PenaltyMessage = { roomId, playerId, reason: 'jjTimeout' };
    this.publish('/app/jjPenalty', payload);
  }
  endgameResolution(roomId: number) {
    const payload: EndgameMessage = { roomId };
    this.publish('/app/endgame', payload);
  }
  drawTwo(roomId: number, playerId: number) {
    const payload: DrawTwoMessage = { roomId, playerId };
    this.publish('/app/drawTwo', payload);
  }
  aaraChangeDrawFour(roomId: number, playerId: number, chosenAara: string) {
    const payload: AaraChangeDrawFourMessage = { roomId, playerId ,color : chosenAara};
    this.publish('/app/colorChangeDrawFour', payload);
  }
  skipTurn(roomId: number, playerId: number) {
    const payload: SkipTurnMessage = { roomId, playerId };
    this.publish('/app/skipTurn', payload);
  }
  reverseTurn(roomId: number, playerId: number) {
    const payload: ReverseTurnMessage = { roomId, playerId };
    this.publish('/app/reverseTurn', payload);
  }
  aaraChange(roomId: number, playerId: number, chosenAara: string) {
    const payload: AaraChangeMessage = { roomId, playerId, color: chosenAara };
    this.publish('/app/aaraChange', payload);
  }
  startGame() {
    if (this.stompClient?.connected) {
      this.stompClient.publish({
        destination: '/app/startGame',
        body: ''   // ✅ no need to send players list
      });
    }
  }
  sendMessage(message: any) {
  if (this.stompClient?.connected) {
    this.stompClient.publish({
        destination: '/app/play',
        body: JSON.stringify(message),
      });
    }
  }
  subscribeToGame(callback: (state: any) => void) {
    this.stompClient.subscribe('/topic/game', (message: any) => {
      callback(JSON.parse(message.body));
    });
  }
}
