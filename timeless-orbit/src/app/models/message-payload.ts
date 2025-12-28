export interface MessagePayload {
  username: string;
  password?: string;  // optional
  score?: number;     // optional
  roomId?: number;     // optional, if needed for room actions
  playerId?: number;   // optional, if backend assigns IDs
}
