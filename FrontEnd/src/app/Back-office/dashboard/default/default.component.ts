import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';

import { MonthlyBarChartComponent } from 'src/app/theme/shared/apexchart/monthly-bar-chart/monthly-bar-chart.component';
import { IncomeOverviewChartComponent } from 'src/app/theme/shared/apexchart/income-overview-chart/income-overview-chart.component';
import { AnalyticsChartComponent } from 'src/app/theme/shared/apexchart/analytics-chart/analytics-chart.component';
import { SalesReportChartComponent } from 'src/app/theme/shared/apexchart/sales-report-chart/sales-report-chart.component';

// Icons
import { IconService, IconDirective } from '@ant-design/icons-angular';
import { FallOutline, GiftOutline, MessageOutline, RiseOutline, SettingOutline} from '@ant-design/icons-angular/icons';
import { CardComponent } from 'src/app/theme/shared/components/card/card.component';
import {
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
  imports: [
    CommonModule,
    RouterModule,
    CardComponent,
    IconDirective,
    MonthlyBarChartComponent,
    IncomeOverviewChartComponent,
    AnalyticsChartComponent,
    SalesReportChartComponent
  ],
  templateUrl: './default.component.html',
  styleUrls: ['./default.component.scss']
})
export class DefaultComponent implements OnInit {
  private iconService = inject(IconService);
  private consultationService = inject(ConsultationService);

  statsLoading = true;
  statsError = '';

  constructor() {
    this.iconService.addIcon(...[RiseOutline, FallOutline, SettingOutline, GiftOutline, MessageOutline,HomeOutline,
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
      BookOutline
    ]);
  }

  // Fake Data for Alzheimer's Application Analytics and Transactions

  recentOrder = [
    { id: 'ORD1234', name: 'Alzheimer’s Medication 1', status: 'Delivered', status_type: 'success', quantity: 3, amount: '$450' },
    { id: 'ORD5678', name: 'Alzheimer’s Medication 2', status: 'Pending', status_type: 'warning', quantity: 2, amount: '$320' },
    { id: 'ORD91011', name: 'Monitoring Device', status: 'Shipped', status_type: 'info', quantity: 1, amount: '$100' },
  ];

  AnalyticEcommerce: Array<{ title: string; amount: string; background: string; border: string; icon: string; percentage?: string; color?: string; number?: string }> = [];

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.statsLoading = true;
    this.statsError = '';
    forkJoin({
      patients: this.consultationService.getPatients(),
      providers: this.consultationService.getProviders(),
      caregivers: this.consultationService.getCaregivers(),
      consultations: this.consultationService.getAllConsultations()
    }).subscribe({
      next: ({ patients, providers, caregivers, consultations }) => {
        const patientsCount = patients?.length ?? 0;
        const providersCount = providers?.length ?? 0;
        const caregiversCount = caregivers?.length ?? 0;
        const consultationsCount = consultations?.length ?? 0;
        const onlineCount = consultations?.filter(c => c.type === 'ONLINE').length ?? 0;
        const completedCount = consultations?.filter(c => c.status === 'COMPLETED').length ?? 0;

        this.AnalyticEcommerce = [
          { title: 'Patients', amount: String(patientsCount), background: 'bg-light-primary ', border: 'border-primary', icon: 'rise' },
          { title: 'Providers', amount: String(providersCount), background: 'bg-light-success ', border: 'border-success', icon: 'rise' },
          { title: 'Caregivers', amount: String(caregiversCount), background: 'bg-light-warning ', border: 'border-warning', icon: 'rise' },
          { title: 'Consultations', amount: String(consultationsCount), background: 'bg-light-info ', border: 'border-info', icon: 'rise', number: `${onlineCount} en ligne, ${completedCount} terminées` }
        ];
        this.statsLoading = false;
      },
      error: (err) => {
        this.statsError = err?.error?.message || err?.message || 'Impossible de charger les statistiques.';
        this.statsLoading = false;
        this.AnalyticEcommerce = [
          { title: 'Patients', amount: '-', background: 'bg-light-primary ', border: 'border-primary', icon: 'fall' },
          { title: 'Providers', amount: '-', background: 'bg-light-success ', border: 'border-success', icon: 'fall' },
          { title: 'Caregivers', amount: '-', background: 'bg-light-warning ', border: 'border-warning', icon: 'fall' },
          { title: 'Consultations', amount: '-', background: 'bg-light-info ', border: 'border-info', icon: 'fall' }
        ];
      }
    });
  }

  transaction = [
    {
      background: 'text-success bg-light-success',
      icon: 'gift',
      title: 'Patient #112233 Medication Order',
      time: 'Today, 2:00 AM',
      amount: '+ $150',
      percentage: '78%'
    },
    {
      background: 'text-primary bg-light-primary',
      icon: 'message',
      title: 'Patient #445566 Medication Order',
      time: '5 August, 1:45 PM',
      amount: '- $180',
      percentage: '8%'
    },
    {
      background: 'text-danger bg-light-danger',
      icon: 'setting',
      title: 'Patient #778899 Monitoring Device',
      time: '7 hours ago',
      amount: '- $320',
      percentage: '16%'
    }
  ];
}
