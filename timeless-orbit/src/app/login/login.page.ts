import { Router } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { WebsocketService } from '../services/websocket.service';
import { PlayerService } from '../services/player.service';
import { MessagePayload } from '../models/message-payload';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.page.html',
  styleUrls: ['./login.page.scss'],
  standalone: false
})
export class LoginPage implements OnInit {

  loginForm!: FormGroup;
  currentUser!: MessagePayload;

  constructor(
    private formBuilder: FormBuilder,
    private router: Router,
    private websocketService: WebsocketService,
    private playerService: PlayerService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.loginForm = this.formBuilder.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  onLogin() {
   if (this.loginForm.valid) {
    const { username, password } = this.loginForm.value;

    // Use PlayerService to add or get player
    this.currentUser = this.playerService.addOrGetPlayer(username, password);

    alert(`Welcome ${username}! 🎉`);
    this.router.navigate(['/home']);

    // Notify backend via WebSocket (avoid sending password ideally)
    this.websocketService.joinLobby(username);
  } else {
  alert('Please enter valid credentials ❌');
  }
}

  // Delegate score update to PlayerService
  updateScore(username: string, points: number) {
    this.playerService.updateScore(username, points);

    this.websocketService.sendMessage({
      type: 'UPDATE_SCORE',
      payload: { username, score: points }
    });
  }

  // Delegate leaderboard to PlayerService
  getLeaderboard(): MessagePayload[] {
    return this.playerService.getLeaderboard();
  }
}
