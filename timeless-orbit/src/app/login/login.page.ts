import { Router } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-login',
  templateUrl: './login.page.html',
  styleUrls: ['./login.page.scss'],
  standalone: false
})
export class LoginPage implements OnInit {

  username: string='';
  password: string='';
  loginForm!: FormGroup;

  constructor(
    private formBuilder: FormBuilder,
    private router: Router
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
      console.log('Login attempt:', username, password);
      // TODO: Hook into authentication service
    }
  }

  checkLogin() {
    if (this.username === 'pratiksha' && this.password === '1234') {
      alert('Login successful! 🎉');
      this.router.navigate(['/home']);
    } else {
      alert('Invalid username or password ❌');
    }
  }

}
