import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-pending',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './pending.html',
})
export class PendingComponent implements OnInit, OnDestroy {
  private interval: any;

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit() {
    this.checkStatus();
    this.interval = setInterval(() => this.checkStatus(), 5000);
  }

  ngOnDestroy() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  }

  checkStatus() {
    this.authService.getSessionStatus().subscribe({
      next: (data: any) => {
        if (data.status === 'APPROVED') {
          this.router.navigate(['/']);
        } else if (data.status === 'REJECTED') {
          this.router.navigate(['/login'], {
            queryParams: { reason: 'rejected' },
          });
        } else if (['MISSING', 'ANONYMOUS'].includes(data.status)) {
          this.router.navigate(['/login']);
        }
      },
      error: () => this.router.navigate(['/login']),
    });
  }

  logout() {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/login']);
    });
  }

  reload() {
    this.checkStatus();
  }
}
