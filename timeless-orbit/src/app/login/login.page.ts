import { Router } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { WebsocketService } from '../services/websocket.service';
import { PlayerService } from '../services/player.service';
import { MessagePayload } from '../models/message-payload';

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
    private playerService: PlayerService
  ) {}

  ngOnInit() {
    this.loginForm = this.formBuilder.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      // Strictly 10 digits, starting with 6-9
      mobile_number: ['', [
        Validators.required,
        Validators.pattern("^[6-9][0-9]{9}$")
      ]],
    });
  }

  onLogin() {
    if (this.loginForm.valid) {
      const { username, mobile_number } = this.loginForm.value;

      // 1. Create/Get the player object via Service
      this.currentUser = this.playerService.addOrGetPlayer(username, mobile_number);

      console.log(this.currentUser.username);
      console.log(this.currentUser.mobile_number);
      // 2. Notify the backend via WebSocket
      // this.websocketService.joinLobby(username);

      // 3. Navigate to Home and pass the complete player object
      this.router.navigate(['/home'], {
        state: { player: this.currentUser }
      });

    } else {
      alert('Please enter a valid 10-digit mobile number ❌');
    }
  }
}
