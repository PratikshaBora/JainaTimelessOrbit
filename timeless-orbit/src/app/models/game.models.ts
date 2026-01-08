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
  hand?: Card[];  // only for current user
  handCount: number;  // for everyone else
}
export interface GameRoomDTO {
  roomId: number;
  players: PlayerDTO[];
  drawPile: Card[];
  discardPile: Card[];
  currentPlayerId: number;
  clockwise: boolean;
  currentAara: string;
  // ✅ Add this line
  turnTimeLeft?: number;
}
