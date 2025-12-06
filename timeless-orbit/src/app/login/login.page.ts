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

  constructor(private fb: FormBuilder) { }

  ngOnInit() {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  onLogin() {
    if (this.loginForm.valid) {
      const { email, password } = this.loginForm.value;
      console.log('Login attempt:', email, password);
      // TODO: Hook into authentication service
    }
  }

  checkLogin() {
    if (this.username === 'pratiksha' && this.password === '1234') {
      alert('Login successful! 🎉');
    } else {
      alert('Invalid username or password ❌');
    }
  }

}
