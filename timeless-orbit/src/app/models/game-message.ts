import { Card } from './game.models';

// Base interface for most game messages
export interface GameMessage {
  roomId: number;
  playerId: number;
}

// --- Specific message types ---
export interface PlayCardMessage extends GameMessage {
  card: Card;
}
export interface DrawCardMessage extends GameMessage {}

export interface PickDiscardMessage extends GameMessage {}

export interface JaiJinendraMessage extends GameMessage {}

export interface PenaltyMessage extends GameMessage {
  reason: string; // e.g. "timeout", "jjTimeout"
}
export interface EndgameMessage {
  roomId: number;
}
export interface DrawTwoMessage extends GameMessage {}

export interface AaraChangeDrawFourMessage extends GameMessage {
  color: string; // chosen aara
}
export interface SkipTurnMessage extends GameMessage {}

export interface ReverseTurnMessage extends GameMessage {}

export interface AaraChangeMessage extends GameMessage {
  color: string; // chosen aara
}
