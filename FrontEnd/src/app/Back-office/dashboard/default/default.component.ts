import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';

import { MonthlyBarChartComponent } from 'src/app/theme/shared/apexchart/monthly-bar-chart/monthly-bar-chart.component';
import { IncomeOverviewChartComponent } from 'src/app/theme/shared/apexchart/income-overview-chart/income-overview-chart.component';
import { AnalyticsChartComponent } from 'src/app/theme/shared/apexchart/analytics-chart/analytics-chart.component';
import { SalesReportChartComponent } from 'src/app/theme/shared/apexchart/sales-report-chart/sales-report-chart.component';

import { NgApexchartsModule, ApexOptions } from 'ng-apexcharts';
import { IconService, IconDirective } from '@ant-design/icons-angular';
import {
  RiseOutline,
  FallOutline,
  SettingOutline,
  GiftOutline,
  MessageOutline,
  DashboardOutline,
  HomeOutline,
  UserOutline,
  UserAddOutline,
  TeamOutline,
  IdcardOutline,
  LockOutline,
  ScheduleOutline,
  CalendarOutline,
  MedicineBoxOutline,
  FilePdfOutline,
  FileTextOutline,
  BookOutline,
} from '@ant-design/icons-angular/icons';
import { ConsultationService } from 'src/app/core/services/consultation.service';

@Component({ 
  selector: 'app-default',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    IconDirective,
    NgApexchartsModule,
    MonthlyBarChartComponent
  ],
  templateUrl: './default.component.html',
  styleUrls: ['./default.component.scss']
})
export class DefaultComponent implements OnInit {
  private iconService = inject(IconService);
  private consultationService = inject(ConsultationService);
  private cdr = inject(ChangeDetectorRef);

  statsLoading = true;
  statsError = '';

  AnalyticEcommerce: any[] = [];
  topPatients: any[] = [];
  topResPatients: any[] = [];
  recentActivities: any[] = [];
  
  chartData: number[] = [0, 0, 0, 0];
  chartLabels: string[] = ['En ligne', 'Présentiel', 'Complété', 'Prévu'];
  
  typeChartOptions!: Partial<ApexOptions>;
  statusChartOptions!: Partial<ApexOptions>;
  resChartOptions!: Partial<ApexOptions>;
  
  monthlyData: number[] = [];
  monthlyLabels: string[] = [];
  statusData: number[] = [];
  statusLabels: string[] = [];

  resStats: any = { pending: 0, accepted: 0, rejected: 0, total: 0 };

  constructor() {
    this.iconService.addIcon(...[
      RiseOutline, FallOutline, SettingOutline, GiftOutline, MessageOutline, HomeOutline,
      DashboardOutline, HomeOutline, UserOutline, UserAddOutline, TeamOutline,
      IdcardOutline, LockOutline, ScheduleOutline, CalendarOutline, MedicineBoxOutline,
      FilePdfOutline, FileTextOutline, BookOutline
    ]);
  }

  ngOnInit(): void {
    setTimeout(() => {
      this.loadStats();
    }, 0);
  }

  loadStats(): void {
    this.statsLoading = true;
    this.statsError = '';
    this.cdr.detectChanges();
    
    forkJoin({
      patients: this.consultationService.getPatients(),
      providers: this.consultationService.getProviders(),
      caregivers: this.consultationService.getCaregivers(),
      consultations: this.consultationService.getAllConsultations(),
      advStats: this.consultationService.getAdminStatistics(),
      resStats: this.consultationService.getReservationStatistics()
    }).subscribe({
      next: ({ patients, providers, caregivers, consultations, advStats, resStats }) => {
        const patientsCount = patients?.length ?? 0;
        const providersCount = providers?.length ?? 0;
        const caregiversCount = caregivers?.length ?? 0;
        const consultationsCount = consultations?.length ?? 0;
        
        const byType = advStats?.byType || {};
        const onlineCount = byType['ONLINE'] || 0;
        const byStatus = advStats?.byStatus || {};
        const completedCount = byStatus['COMPLETED'] || 0;

        this.topPatients = advStats?.topPatients || [];
        this.topResPatients = resStats?.topPatients || [];
        
        this.resStats = {
          pending: resStats?.byStatus?.['PENDING'] || 0,
          accepted: resStats?.byStatus?.['ACCEPTED'] || 0,
          rejected: resStats?.byStatus?.['REJECTED'] || 0,
          total: resStats?.total || 0
        };

        // Map real consultations to recent activities
        this.recentActivities = (consultations ?? []).slice(0, 8).map(c => ({
          title: c.title,
          time: new Date(c.startTime).toLocaleString('fr-FR', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }),
          type: c.type,
          status: c.status,
          patientId: c.patientId
        }));

        this.AnalyticEcommerce = [
          { title: 'Base Patients', amount: String(patientsCount), background: 'bg-light-primary ', icon: 'user' },
          { title: 'Total Consultations', amount: String(consultationsCount), background: 'bg-light-success ', icon: 'schedule' },
          { title: 'Réservations Archivées', amount: String(this.resStats.total), background: 'bg-light-warning ', icon: 'calendar' },
          { title: 'Consultations en Cours', amount: String(byStatus['ONGOING'] || 0), background: 'bg-light-info ', icon: 'medicine-box' }
        ];

        this.updateChartData(byType, byStatus, advStats?.monthlyDistribution);
        this.initExtraCharts(byType, byStatus, resStats?.byStatus);
        this.statsLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.statsError = err?.error?.message || err?.message || 'Impossible de charger les statistiques.';
        this.statsLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  updateChartData(byType: any, byStatus: any, monthlyDist: any): void {
    this.chartData = [
      byType['ONLINE'] || 0,
      byType['PRESENTIAL'] || 0,
      byStatus['COMPLETED'] || 0,
      byStatus['SCHEDULED'] || 0
    ];

    if (monthlyDist) {
      this.monthlyLabels = Object.keys(monthlyDist);
      this.monthlyData = Object.values(monthlyDist);
    }

    if (byStatus) {
      this.statusLabels = Object.keys(byStatus);
      this.statusData = Object.values(byStatus);
    }
  }

  private initExtraCharts(byType: any, byStatus: any, resByStatus: any): void {
    const typeValues = [byType['ONLINE'] || 0, byType['PRESENTIAL'] || 0];
    this.typeChartOptions = {
      series: typeValues,
      chart: { type: 'donut', height: 220, animations: { enabled: true, speed: 800 } },
      labels: ['En ligne', 'Présentiel'],
      colors: ['#4f46e5', '#10b981'],
      legend: { position: 'bottom' },
      dataLabels: { enabled: false },
      plotOptions: { pie: { donut: { size: '75%', labels: { show: true, total: { show: true, label: 'Type' } } } } }
    };

    const statusValues = Object.values(byStatus);
    const statusKeys = Object.keys(byStatus);
    this.statusChartOptions = {
      series: statusValues as number[],
      chart: { type: 'pie', height: 220 },
      labels: statusKeys,
      colors: ['#6366f1', '#0ea5e9', '#f59e0b', '#f43f5e'],
      legend: { position: 'bottom' },
      dataLabels: { enabled: true }
    };

    // New Reservation Chart
    if (resByStatus) {
      const resKeys = ['ACCEPTED', 'PENDING', 'REJECTED'];
      const resValues = resKeys.map(k => resByStatus[k] || 0);
      this.resChartOptions = {
        series: resValues,
        chart: { type: 'donut', height: 220 },
        labels: ['Acceptées', 'En attente', 'Rejetées'],
        colors: ['#10b981', '#f59e0b', '#ef4444'],
        legend: { position: 'bottom' },
        plotOptions: { pie: { donut: { size: '70%', labels: { show: true, total: { show: true, label: 'Réservations' } } } } }
      };
    }
  }
}
