import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { MessagePayload } from '../models/message-payload';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth'; // adjust to your backend

  constructor(private http: HttpClient) {}

  login(credentials: { username: string; password: string }): Observable<MessagePayload> {
    return this.http.post<MessagePayload>(`${this.apiUrl}/login`, credentials);
  }

}
