import {
  Component,
  OnInit,
  AfterViewInit,
  OnDestroy,
  ElementRef,
  QueryList,
  ViewChildren,
  NgZone,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { AnalysisService } from '../../../../core/services/analysis.service';
import { Chart, registerables } from 'chart.js';
import {
  catchError,
  finalize,
  of,
  timeout,
  Subject,
  take,
  takeUntil,
} from 'rxjs';
import { AnalysisJob, User } from '../../../../core/models';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar';
import { environment } from '../../../../../environments/environment';

Chart.register(...registerables);

interface ProcessedRole {
  name: string;
  jobs: AnalysisJob[];
}

interface ProcessedCompany {
  name: string;
  roles: ProcessedRole[];
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, NavbarComponent],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css'],
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  
  user = computed(() => this.authService.currentUser());
  processedCompanies = signal<ProcessedCompany[]>([]);
  loading = signal(true);
  dashboardError = signal<string | null>(null);

  showDeleteModal = signal(false);
  showProcessingModal = signal(false);
  deleteTargetCompany = signal('');
  deleteTargetRole = signal('');

  private destroy$ = new Subject<void>();
  private loadingTimeout: any;
  private renderTimeout: any;
  private canvasSub: any;
  private charts: any[] = [];
  private isRendering = false;
  private eventSource: EventSource | null = null;

  @ViewChildren('chartCanvas') chartCanvases!: QueryList<
    ElementRef<HTMLCanvasElement>
  >;

  constructor(
    private authService: AuthService,
    private analysisService: AnalysisService,
    public router: Router,
    private ngZone: NgZone,
  ) {}

  ngOnInit() {
    this.checkSession();
  }

  ngAfterViewInit() {
    this.canvasSub = this.chartCanvases.changes
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (!this.isRendering && this.processedCompanies().length > 0) {
          this.renderCharts();
        }
      });

    if (this.processedCompanies().length > 0) {
      this.renderCharts();
    }
  }

  private setupSSE() {
    if (this.eventSource) return;

    const token = localStorage.getItem('auth_token');
    if (!token || token === 'null') {
      return;
    }

    const url = `/api/dashboard/events?token=${token}`;
    this.eventSource = new EventSource(url);

    this.eventSource.onmessage = (event) => {
      this.ngZone.run(() => {
        if (event.data === 'updated') {
          this.loadDashboardData();
        }
      });
    };

    this.eventSource.onerror = () => {
      this.closeSSE();
      setTimeout(() => this.setupSSE(), 5000);
    };
  }

  private closeSSE() {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  checkSession() {
    this.authService
      .getSessionStatus()
      .pipe(
        take(1),
        timeout(5000),
        takeUntil(this.destroy$),
        catchError(() => of({ status: 'ANONYMOUS' })),
      )
      .subscribe({
        next: (data) => {
          if (data.status === 'PENDING_APPROVAL') {
            this.loading.set(false);
            this.router.navigate(['/pending']);
          } else if (
            ['MISSING', 'REJECTED', 'ANONYMOUS'].includes(data.status)
          ) {
            this.loading.set(false);
            this.router.navigate(['/login']);
          } else {
            this.loadUser();
            this.loadDashboardData();
          }
        },
        error: () => {
          this.loading.set(false);
          this.router.navigate(['/login']);
        },
      });
  }

  loadUser() {
    this.authService
      .getCurrentUser()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.setupSSE();
        },
        error: () => {},
      });
  }

  loadDashboardData() {
    this.loading.set(true);
    this.dashboardError.set(null);

    if (this.loadingTimeout) clearTimeout(this.loadingTimeout);
    this.loadingTimeout = setTimeout(() => {
      if (this.loading()) {
        this.loading.set(false);
        this.dashboardError.set(
          'A consulta demorou demais. Verifique sua conexão e tente novamente.',
        );
      }
    }, 12000);

    this.analysisService
      .getJobs()
      .pipe(
        timeout(10000),
        takeUntil(this.destroy$),
        catchError(() => {
          this.dashboardError.set(
            'Não foi possível carregar os Talent Insights agora.',
          );
          return of({});
        }),
        finalize(() => {
          if (this.loadingTimeout) clearTimeout(this.loadingTimeout);
          this.loading.set(false);
        }),
      )
      .subscribe((data: any) => {
        this.processDashboardData(data);
      });
  }

  private processDashboardData(data: any) {
    if (!data || Object.keys(data).length === 0) {
      this.processedCompanies.set([]);
      return;
    }

    const companies = Object.entries(data).map(
      ([companyName, roles]: [string, any]) => ({
        name: companyName,
        roles: Object.entries(roles).map(([roleName, jobs]: [string, any]) => ({
          name: roleName,
          jobs: jobs as AnalysisJob[],
        })),
      }),
    );

    this.processedCompanies.set(companies);
  }

  renderCharts() {
    if (this.isRendering || this.processedCompanies().length === 0) return;

    this.isRendering = true;

    this.charts.forEach((c) => {
      try {
        c.destroy();
      } catch (e) {}
    });
    this.charts = [];

    if (this.renderTimeout) clearTimeout(this.renderTimeout);

    this.renderTimeout = setTimeout(() => {
      this.ngZone.runOutsideAngular(() => {
        try {
          if (this.destroy$.isStopped) return;

          const canvasMap = new Map<string, HTMLCanvasElement>();
          this.chartCanvases.forEach((c) => {
            canvasMap.set(c.nativeElement.id, c.nativeElement);
          });

          this.processedCompanies().forEach((company) => {
            company.roles.forEach((role) => {
              const chartId = `chart-${company.name}-${role.name}`;
              const canvas = canvasMap.get(chartId);
              if (canvas) {
                try {
                  const chart = new Chart(canvas, {
                    type: 'bar',
                    data: {
                      labels: role.jobs.map(
                        (j) => j.originalFileName.split('.')[0],
                      ),
                      datasets: [
                        {
                          label: 'Match Score %',
                          data: role.jobs.map((j) => j.matchScore || 0),
                          backgroundColor: role.jobs.map((j) => {
                            const score = j.matchScore || 0;
                            return score >= 80
                              ? '#10b981'
                              : score >= 50
                                ? '#f59e0b'
                                : '#ef4444';
                          }),
                          borderRadius: 8,
                        },
                      ],
                    },
                    options: {
                      indexAxis: 'y',
                      responsive: true,
                      maintainAspectRatio: false,
                      plugins: { legend: { display: false } },
                      scales: {
                        x: { min: 0, max: 100 },
                        y: { ticks: { font: { weight: 'bold', size: 10 } } },
                      },
                    },
                  });
                  this.charts.push(chart);
                } catch (e) {}
              }
            });
          });
        } finally {
          this.isRendering = false;
        }
      });
    }, 200);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();

    if (this.loadingTimeout) clearTimeout(this.loadingTimeout);
    if (this.renderTimeout) clearTimeout(this.renderTimeout);
    this.closeSSE();

    this.charts.forEach((c) => {
      try {
        c.destroy();
      } catch (e) {}
    });
    this.charts = [];
  }

  getScoreColor(job: AnalysisJob) {
    if (job.status !== 'COMPLETED') return 'text-slate-400';
    const score = job.matchScore || 0;
    return score >= 80
      ? 'text-emerald-600'
      : score >= 50
        ? 'text-amber-600'
        : 'text-rose-600';
  }

  getScoreBg(job: AnalysisJob) {
    if (job.status !== 'COMPLETED') return 'bg-slate-50';
    const score = job.matchScore || 0;
    return score >= 80
      ? 'bg-emerald-50'
      : score >= 50
        ? 'bg-amber-50'
        : 'bg-rose-50';
  }

  canOpenAnalysis(job: AnalysisJob): boolean {
    return !!job?.id && job?.status === 'COMPLETED';
  }

  openAnalysis(job: AnalysisJob) {
    if (!job?.id) return;

    if (job.status === 'FAILED') {
      alert('Esta análise falhou e não possui resultado para abrir.');
      return;
    }

    if (job.status !== 'COMPLETED') {
      this.showProcessingModal.set(true);
      return;
    }

    this.router.navigate(['/'], { queryParams: { jobId: job.id } });
  }

  reloadDashboard() {
    this.loadDashboardData();
  }

  confirmDelete(company: string, role: string) {
    this.deleteTargetCompany.set(company);
    this.deleteTargetRole.set(role);
    this.showDeleteModal.set(true);
  }

  cancelDelete() {
    this.showDeleteModal.set(false);
    this.deleteTargetCompany.set('');
    this.deleteTargetRole.set('');
  }

  executeDelete() {
    const company = this.deleteTargetCompany();
    const role = this.deleteTargetRole();
    if (!company || !role) return;

    this.analysisService
      .deleteAnalysis(company, role)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.cancelDelete();
          this.loadDashboardData();
        },
        error: (err) => {
          this.cancelDelete();
          alert('Erro ao excluir análise.');
          console.error(err);
        },
      });
  }

  downloadReport(company: string, role: string) {
    this.analysisService
      .exportReport(company, role)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `Relatorio_${company.replace(/\s+/g, '_')}_${role.replace(/\s+/g, '_')}.pdf`;
          document.body.appendChild(a);
          a.click();
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
        },
        error: (err) => {
          alert('Erro ao gerar relatório PDF.');
          console.error(err);
        },
      });
  }
}
