import { Component, OnDestroy, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { AnalysisService } from '../../../../core/services/analysis.service';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar';
import { Subject, takeUntil } from 'rxjs';
import { AnalysisJob, ResumeAnalysis, User } from '../../../../core/models';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, RouterLink],
  templateUrl: './home.html',
  styleUrls: ['./home.css'],
})
export class HomeComponent implements OnInit, OnDestroy {
  
  user = computed(() => this.authService.currentUser());

  company = signal('');
  role = signal('');
  jobDescription = signal('');
  jobDescriptionLength = computed(() => this.jobDescription().length);
  isDescriptionTooLong = computed(() => this.jobDescriptionLength() > 4000);
  selectedFiles = signal<File[]>([]);

  isAnalyzing = signal(false);
  currentJobId = signal<string | null>(null);
  analysisResult = signal<ResumeAnalysis | null>(null);
  errorMessage = signal<string | null>(null);

  private pollIntervalId: any = null;
  private destroy$ = new Subject<void>();

  constructor(
    private authService: AuthService,
    private analysisService: AnalysisService,
    private route: ActivatedRoute,
    public router: Router,
  ) {}

  ngOnInit() {
    this.checkSession();
    this.loadUser();
    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe((params) => {
        if (params['jobId']) {
          this.loadSpecificJob(params['jobId']);
        } else {
          this.resetState();
        }
      });
  }

  resetState() {
    this.currentJobId.set(null);
    this.analysisResult.set(null);
    this.errorMessage.set(null);
    this.company.set('');
    this.role.set('');
    this.jobDescription.set('');
    this.selectedFiles.set([]);
    this.stopPolling();
  }

  private stopPolling() {
    if (this.pollIntervalId) {
      clearInterval(this.pollIntervalId);
      this.pollIntervalId = null;
    }
  }

  loadSpecificJob(jobId: string) {
    this.isAnalyzing.set(true);
    this.currentJobId.set(jobId);
    this.analysisResult.set(null);
    this.errorMessage.set(null);

    this.analysisService
      .getJobStatus(jobId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (job: AnalysisJob) => {
          if (job.status === 'COMPLETED' && job.resultJson) {
            this.analysisResult.set(JSON.parse(job.resultJson));
            this.company.set(job.company);
            this.role.set(job.role);
          } else if (job.status === 'FAILED') {
            this.handleJobFailure(job);
          }
          this.isAnalyzing.set(false);
        },
        error: () => {
          this.isAnalyzing.set(false);
          this.errorMessage.set('Não foi possível carregar os dados desta análise.');
        },
      });
  }

  private handleJobFailure(job: AnalysisJob) {
    try {
      const errorData = JSON.parse(job.resultJson || '{}');
      let msg = errorData.error || 'Erro desconhecido na análise.';
      
      if (msg.toLowerCase().includes('rate limit') || msg.toLowerCase().includes('quota') || msg.includes('429')) {
        msg = 'O limite de processamento da IA foi atingido. Por favor, aguarde alguns instantes ou tente com uma descrição menor.';
      } else if (msg.toLowerCase().includes('token') || msg.toLowerCase().includes('context length')) {
        msg = 'O texto é muito longo para ser processado pela IA. Tente reduzir a descrição da vaga.';
      }

      this.errorMessage.set(msg);
    } catch (e) {
      this.errorMessage.set('Erro ao processar o resultado da análise.');
    }
  }

  ngOnDestroy() {
    this.stopPolling();
    this.destroy$.next();
    this.destroy$.complete();
  }

  checkSession() {
    this.authService
      .getSessionStatus()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          if (data.status === 'PENDING_APPROVAL') {
            this.router.navigate(['/pending']);
          } else if (
            ['MISSING', 'REJECTED', 'ANONYMOUS'].includes(data.status)
          ) {
            this.router.navigate(['/login']);
          }
        },
        error: () => this.router.navigate(['/login']),
      });
  }

  loadUser() {
    this.authService
      .getCurrentUser()
      .pipe(takeUntil(this.destroy$))
      .subscribe();
  }

  isAdmin(): boolean {
    const user = this.user();
    if (user && user.role) {
      return user.role.includes('ADMIN');
    }
    const role = localStorage.getItem('user_role');
    return role ? role.includes('ADMIN') : false;
  }

  handleFileSelect(event: any) {
    const files = Array.from(event.target.files) as File[];
    const currentFiles = this.selectedFiles();

    const newFiles = files.filter(
      (file) =>
        !currentFiles.find((f) => f.name === file.name && f.size === file.size),
    );

    this.selectedFiles.update((prev) => [...prev, ...newFiles]);
  }

  removeFile(index: number) {
    this.selectedFiles.update((prev) => {
      const next = [...prev];
      next.splice(index, 1);
      return next;
    });
  }

  processAnalysis() {
    if (this.selectedFiles().length === 0) return;

    this.isAnalyzing.set(true);
    this.analysisResult.set(null);
    this.errorMessage.set(null);

    const formData = new FormData();
    formData.append('company', this.company());
    formData.append('role', this.role());
    formData.append('jobDescription', this.jobDescription());
    this.selectedFiles().forEach((file) => formData.append('file', file));

    this.analysisService
      .analyze(formData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: AnalysisJob | AnalysisJob[]) => {
          if (Array.isArray(response) && response.length > 1) {
            this.router.navigate(['/dashboard']);
            return;
          }
          const job = Array.isArray(response) ? response[0] : response;
          this.currentJobId.set(job.id);
          this.pollStatus();
        },
        error: () => {
          this.isAnalyzing.set(false);
          this.errorMessage.set('Erro ao iniciar o processamento.');
        },
      });
  }

  pollStatus() {
    const jobId = this.currentJobId();
    if (!jobId) return;

    this.stopPolling();

    this.pollIntervalId = setInterval(() => {
      this.analysisService
        .getJobStatus(jobId)
        .pipe(takeUntil(this.destroy$))
        .subscribe((job: AnalysisJob) => {
          if (job.status === 'COMPLETED' && job.resultJson) {
            this.stopPolling();
            this.analysisResult.set(JSON.parse(job.resultJson));
            this.isAnalyzing.set(false);
          } else if (job.status === 'FAILED') {
            this.stopPolling();
            this.handleJobFailure(job);
            this.isAnalyzing.set(false);
          }
        });
    }, 2000);
  }

  startNewAnalysis() {
    this.resetState();
    this.router.navigate(['/']);
  }

  logout() {
    this.authService
      .logout()
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.router.navigate(['/login']);
      });
  }
}
