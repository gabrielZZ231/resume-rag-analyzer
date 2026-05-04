import { Routes } from '@angular/router';
import { HomeComponent } from './features/analysis/pages/home/home';
import { LoginComponent } from './features/auth/pages/login/login';
import { RegisterComponent } from './features/auth/pages/register/register';
import { DashboardComponent } from './features/dashboard/pages/dashboard/dashboard';
import { AdminComponent } from './features/admin/pages/admin/admin';
import { PendingComponent } from './features/auth/pages/pending/pending';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'admin', component: AdminComponent },
  { path: 'pending', component: PendingComponent },
  { path: '**', redirectTo: '' },
];
