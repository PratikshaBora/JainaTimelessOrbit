export interface MessagePayload {
  id: number;
  username: string;
  mobile_number?: string;  // optional
  points?: number;     // optional
  roomId?: number;     // optional, if needed for room actions
  // playerId?: number;   // optional, if backend assigns IDs
}
