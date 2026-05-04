import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getPendingUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/pending-users`, { withCredentials: true });
  }

  getApprovedUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/users`, { withCredentials: true });
  }

  approveUser(id: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/approve/${id}`, {}, { withCredentials: true });
  }

  rejectUser(id: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/reject/${id}`, {}, { withCredentials: true });
  }

  updateUserRole(id: string, role: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/users/${id}/role?role=${role}`, {}, { withCredentials: true });
  }

  deleteUser(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/admin/users/${id}`, { withCredentials: true });
  }
}
