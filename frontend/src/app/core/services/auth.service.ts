import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, of, catchError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User } from '../models';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private apiUrl = environment.apiUrl;

  currentUser = signal<User | null>(null);

  constructor(private http: HttpClient) {}

  getSessionStatus(): Observable<{ status: string }> {
    const token = localStorage.getItem('auth_token');
    if (!token) return of({ status: 'ANONYMOUS' });
    return this.http
      .get<{ status: string }>(`${this.apiUrl}/auth/session-status`)
      .pipe(catchError(() => of({ status: 'ANONYMOUS' })));
  }

  getCurrentUser(): Observable<User> {
    return this.http
      .get<User>(`${this.apiUrl}/dashboard/me`)
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  login(credentials: any): Observable<any> {
    return this.http
      .post(`${this.apiUrl}/auth/login`, {
        email: credentials.username,
        password: credentials.password,
      })
      .pipe(
        tap((res: any) => {
          if (res.token) {
            localStorage.setItem('auth_token', res.token);
            localStorage.setItem('user_email', res.email);
            localStorage.setItem('user_role', res.role);
            localStorage.setItem('user_status', res.status);
          }
        }),
      );
  }

  register(user: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/register`, user);
  }

  logout(): Observable<void> {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('user_email');
    localStorage.removeItem('user_role');
    this.currentUser.set(null);
    return of(undefined);
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('auth_token');
  }
}
