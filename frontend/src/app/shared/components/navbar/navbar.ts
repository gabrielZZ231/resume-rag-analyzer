import { Component, OnInit, OnDestroy, Input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Subject, takeUntil } from 'rxjs';
import { User } from '../../../core/models';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.html',
})
export class NavbarComponent implements OnInit, OnDestroy {
  
  @Input() set user(val: User | null) {
    if (val) this.authService.currentUser.set(val);
  }

  currentUser = computed(() => this.authService.currentUser());

  notifications: any[] = [];
  unreadCount = 0;
  showNotifDropdown = false;
  showProfileDropdown = false;
  private destroy$ = new Subject<void>();

  constructor(
    private authService: AuthService,
    private notificationService: NotificationService,
    public router: Router,
  ) {}

  ngOnInit() {
    if (this.isAdmin()) {
      this.loadNotifications();
      this.loadUnreadCount();
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  isAdmin(): boolean {
    const user = this.currentUser();
    if (user && user.role) {
      return user.role.includes('ADMIN');
    }
    const role = localStorage.getItem('user_role');
    return role ? role.includes('ADMIN') : false;
  }

  loadNotifications() {
    this.notificationService
      .getNotifications()
      .pipe(takeUntil(this.destroy$))
      .subscribe((notifs) => {
        this.notifications = notifs;
      });
  }

  loadUnreadCount() {
    this.notificationService
      .getUnreadCount()
      .pipe(takeUntil(this.destroy$))
      .subscribe((count) => {
        this.unreadCount = count;
      });
  }

  toggleNotifDropdown() {
    this.showNotifDropdown = !this.showNotifDropdown;
    if (this.showNotifDropdown) {
      this.loadNotifications();
    }
  }

  toggleProfileDropdown() {
    this.showProfileDropdown = !this.showProfileDropdown;
  }

  markReadAndGo(notifId: string, userId: string) {
    this.notificationService.markAsRead(notifId).subscribe(() => {
      this.showNotifDropdown = false;
      this.router.navigate(['/admin'], { queryParams: { highlight: userId } });
    });
  }

  readAllNotifications() {
    this.notificationService.readAll().subscribe(() => {
      this.loadNotifications();
      this.loadUnreadCount();
    });
  }

  goToHome() {
    this.showProfileDropdown = false;
    this.router.navigate(['/'], { queryParams: {} });
  }

  logout() {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/login']);
    });
  }
}
