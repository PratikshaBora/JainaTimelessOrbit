import { Injectable } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import * as SockJS from 'sockjs-client';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class WebsocketService {
  private stompClient!: Client;
  private playersSubject = new BehaviorSubject<string[]>([]);
  players$ = this.playersSubject.asObservable();

  connect(callback: Function|null=null) {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      reconnectDelay: 5000,
      debug: (msg) => console.log(msg),
    });

    this.stompClient.onConnect = (frame) => {
      console.log('Connected: ' + frame);

      // Subscribe to lobby updates
      this.stompClient.subscribe('/topic/lobby', (message: IMessage) => {
        const playerName = JSON.parse(message.body).username;
        const currentPlayers = this.playersSubject.value;
        if (!currentPlayers.includes(playerName)) {
          this.playersSubject.next([...currentPlayers, playerName]);
        }
      });

      // Subscribe to game updates
      this.stompClient.subscribe('/topic/game', (message: IMessage) => {
        console.log('Game update:', JSON.parse(message.body));
      });
    };

    this.stompClient.activate();
    if(callback)
    {
      callback();
    }
  }

  disconnect() {
    if (this.stompClient) {
      this.stompClient.deactivate();
    }
  }

  // --- Lobby actions ---
  joinLobby(username: string) {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: '/joinLobby',
        body: JSON.stringify({ username, password: '' }),
      });
    }
  }


  // --- Game actions ---
  playCard(roomId: number, playerId: number, card: any) {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: '/app/playCard',
        body: JSON.stringify({ roomId, playerId, card }),
      });
    }
  }

  drawCard(roomId: number, playerId: number) {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: '/app/drawCard',
        body: JSON.stringify({ roomId, playerId }),
      });
    }
  }

  pickDiscard(roomId: number, playerId: number) {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: '/app/pickDiscard',
        body: JSON.stringify({ roomId, playerId }),
      });
    }
  }

  jaiJinendra(roomId: number, playerId: number) {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: '/app/jaiJinendra',
        body: JSON.stringify({ roomId, playerId }),
      });
    }
  }

  autoPenaltyDraw(roomId: number, playerId: number) {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: '/app/penaltyDraw',
        body: JSON.stringify({ roomId, playerId, reason: 'timeout' }),
      });
    }
  }

  jjTimeoutPenalty(roomId: number, playerId: number) {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: '/app/jjPenalty',
        body: JSON.stringify({ roomId, playerId, reason: 'jjTimeout' }),
      });
    }
  }

  endgameResolution(roomId: number) {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: '/app/endgame',
        body: JSON.stringify({ roomId }),
      });
    }
  }

  drawTwo(roomId: number, playerId: number) {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: '/app/drawTwo',
        body: JSON.stringify({ roomId, playerId }),
      });
    }
  }

  drawFour(roomId: number, playerId: number) {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: '/app/drawFour',
        body: JSON.stringify({ roomId, playerId }),
      });
    }
  }

  // --- Lobby actions ---
  createRoom(players: string[]) {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: '/app/createRoom',
        body: JSON.stringify({ players })
      });
    }
  }
}
