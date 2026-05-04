import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
})
export class LoginComponent implements OnInit {
  username = '';
  password = '';
  error: string | null = null;
  verified = false;
  rejected = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe((params) => {
      this.verified = params['verified'] === 'true';
      this.rejected =
        params['rejected'] === 'true' || params['reason'] === 'rejected';
      if (params['error']) {
        this.error =
          params['error'] === 'forbidden'
            ? 'Acesso negado. Sua conta pode estar aguardando aprovação ou você não tem permissão para esta área.'
            : 'E-mail ou senha incorretos. Verifique suas credenciais.';
      }
    });
  }

  onSubmit() {
    this.error = null;

    const credentials = {
      username: this.username.trim(),
      password: this.password.trim(),
    };

    this.authService.login(credentials).subscribe({
      next: (res: any) => {
        if (res.status === 'PENDING_APPROVAL') {
          this.router.navigate(['/pending']);
        } else if (res.role === 'ADMIN') {
          this.router.navigate(['/admin']);
        } else {
          this.router.navigate(['/']);
        }
      },
      error: (err: HttpErrorResponse) => {
        const backendError = (err?.error?.error || err?.error || '')
          .toString()
          .toLowerCase();

        if (backendError.includes('não verificado')) {
          this.error =
            'Seu e-mail ainda não foi verificado. Verifique sua caixa de entrada.';
          return;
        }

        if (backendError.includes('recusada')) {
          this.error =
            'Sua solicitação de acesso foi recusada pelo administrador.';
          return;
        }

        if (err.status === 403) {
          this.error = 'Acesso restrito. Verifique seu status ou e-mail.';
          return;
        }

        if (backendError.includes('inativa')) {
          this.error =
            'Sua conta está inativa. Entre em contato com o administrador.';
          return;
        }

        if (
          err.status === 401 ||
          err.status === 400 ||
          err.status === 403 ||
          backendError.includes('login ou senha') ||
          backendError.includes('credenciais') ||
          backendError.includes('incorretos') ||
          backendError.includes('invalid') ||
          backendError.includes('authorized')
        ) {
          this.error = 'Login ou senha incorretos.';
          return;
        }

        this.error =
          'Não foi possível realizar login agora. Tente novamente em instantes.';
      },
    });
  }
}
