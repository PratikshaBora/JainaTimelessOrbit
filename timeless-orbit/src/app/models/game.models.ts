// game.models.ts
export interface Card {
  id: number;
  aara: string;       // color (first…sixth)
  dwar: string;       // symbol/action
  type: string;       // e.g. "NUMBER", "WILD"
  pointValue: number;
  imageURL: string;   // ✅ card image
}
export interface PlayerDTO {
  id: number;
  username: string;
  points: number;
  hand: Card[];
}
export interface GameRoomDTO {
  roomId: number;
  players: PlayerDTO[];
  drawPile: Card[];
  discardPile: Card[];
  currentPlayerId: number;
  clockwise: boolean;
  currentAara: string | null;
}
