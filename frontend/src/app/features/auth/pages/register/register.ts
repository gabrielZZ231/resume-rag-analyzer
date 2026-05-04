import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.html',
})
export class RegisterComponent {
  email = '';
  password = '';
  success = false;
  error: string | null = null;

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  onSubmit() {
    this.authService
      .register({ email: this.email, password: this.password })
      .subscribe({
        next: () => {
          this.success = true;
          this.error = null;
        },
        error: (err) => {
          this.error = 'Ocorreu um erro ao processar sua solicitação.';
          this.success = false;
        },
      });
  }
}
