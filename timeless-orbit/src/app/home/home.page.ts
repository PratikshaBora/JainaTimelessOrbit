import { Router } from '@angular/router';
import { Component } from '@angular/core';

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
  standalone: false,
})
export class HomePage {
  currentPosition = 'Not Started';

  constructor(private router: Router) {}

  startGame() {
    this.currentPosition = 'Waiting in Lobby';
    this.router.navigate(['/lobby']);  // Navigate to lobby page
  }

}
