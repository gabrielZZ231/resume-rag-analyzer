import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { AdminService } from '../../../../core/services/admin.service';
import { NotificationService } from '../../../../core/services/notification.service';

import { NavbarComponent } from '../../../../shared/components/navbar/navbar';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, RouterLink, NavbarComponent],
  templateUrl: './admin.html',
  styleUrls: ['./admin.css'],
})
export class AdminComponent implements OnInit {
  user: any = null;
  pendingUsers: any[] = [];
  approvedUsers: any[] = [];
  notifications: any[] = [];
  unreadCount = 0;
  showNotifDropdown = false;
  showProfileDropdown = false;
  activeTab: 'triage' | 'manage' = 'triage';
  highlightId: string | null = null;

  constructor(
    private authService: AuthService,
    private adminService: AdminService,
    private notificationService: NotificationService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    this.checkSession();
    this.route.queryParams.subscribe((params) => {
      this.highlightId = params['highlight'];
    });
  }

  checkSession() {
    this.authService.getSessionStatus().subscribe({
      next: (data: any) => {
        if (data.status === 'PENDING_APPROVAL') {
          this.router.navigate(['/pending']);
        } else if (['MISSING', 'REJECTED', 'ANONYMOUS'].includes(data.status)) {
          this.router.navigate(['/login']);
        } else {
          this.loadUser();
          this.loadPendingUsers();
        }
      },
      error: () => this.router.navigate(['/login']),
    });
  }

  loadUser() {
    this.authService.getCurrentUser().subscribe({
      next: (user) => {
        this.user = user;
        this.loadNotifications();
        this.loadUnreadCount();
      },
      error: () => {},
    });
  }

  loadPendingUsers() {
    this.adminService.getPendingUsers().subscribe((users) => {
      this.pendingUsers = users;
    });
  }

  loadApprovedUsers() {
    this.adminService.getApprovedUsers().subscribe((users) => {
      this.approvedUsers = users;
    });
  }

  switchTab(tab: 'triage' | 'manage') {
    this.activeTab = tab;
    if (tab === 'manage') {
      this.loadApprovedUsers();
    } else {
      this.loadPendingUsers();
    }
  }

  approveUser(id: string) {
    this.adminService.approveUser(id).subscribe(() => {
      this.loadPendingUsers();
    });
  }

  rejectUser(id: string) {
    this.adminService.rejectUser(id).subscribe(() => {
      this.loadPendingUsers();
    });
  }

  updateRole(id: string, role: string) {
    this.adminService.updateUserRole(id, role).subscribe(() => {
      this.loadApprovedUsers();
    });
  }

  deleteUser(id: string) {
    if (confirm('Deseja realmente remover este usuário do sistema?')) {
      this.adminService.deleteUser(id).subscribe(() => {
        this.loadApprovedUsers();
      });
    }
  }

  loadNotifications() {
    this.notificationService.getNotifications().subscribe((notifs) => {
      this.notifications = notifs;
    });
  }

  loadUnreadCount() {
    this.notificationService.getUnreadCount().subscribe((count) => {
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

  readAllNotifications() {
    this.notificationService.readAll().subscribe(() => {
      this.loadNotifications();
      this.loadUnreadCount();
    });
  }

  markReadAndGo(notifId: string, userId: string) {
    this.notificationService.markAsRead(notifId).subscribe(() => {
      this.highlightId = userId;
      this.showNotifDropdown = false;
    });
  }

  logout() {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/login']);
    });
  }

  isAdmin() {
    if (this.user && this.user.role) {
      return this.user.role.includes('ADMIN');
    }
    const role = localStorage.getItem('user_role');
    return role ? role.includes('ADMIN') : false;
  }
}
