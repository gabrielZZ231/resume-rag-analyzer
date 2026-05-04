import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AnalysisJob } from '../models';

@Injectable({
  providedIn: 'root',
})
export class AnalysisService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  analyze(formData: FormData): Observable<AnalysisJob[]> {
    return this.http.post<AnalysisJob[]>(`${this.apiUrl}/analyze`, formData, {
      withCredentials: true,
    });
  }

  getJobStatus(jobId: string): Observable<AnalysisJob> {
    return this.http.get<AnalysisJob>(`${this.apiUrl}/analyze/${jobId}`, {
      withCredentials: true,
    });
  }

  getJobs(): Observable<AnalysisJob[]> {
    return this.http.get<AnalysisJob[]>(`${this.apiUrl}/dashboard/jobs`, {
      withCredentials: true,
    });
  }

  deleteAnalysis(company: string, role: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/dashboard/jobs`, {
      params: { company, role },
      withCredentials: true,
    });
  }

  exportReport(company: string, role: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/dashboard/report`, {
      params: { company, role },
      responseType: 'blob',
      withCredentials: true,
    });
  }
}
