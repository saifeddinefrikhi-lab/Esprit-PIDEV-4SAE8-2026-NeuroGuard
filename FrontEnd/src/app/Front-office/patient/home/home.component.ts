import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AlertService } from 'src/app/core/services/alert.service';
import { MedicalHistoryService } from 'src/app/core/services/medical-history.service';
import { AlertResponse } from 'src/app/core/models/alert.model';
import { MedicalHistoryResponse } from 'src/app/core/models/medical-history.model';

import { IconService, IconDirective } from '@ant-design/icons-angular';
import { RiseOutline, FallOutline } from '@ant-design/icons-angular/icons';
import { CardComponent } from 'src/app/theme/shared/components/card/card.component';

interface ChartPoint {
  label: string;
  value: number;
  percent: number;
  colorClass: string;
}

@Component({
  selector: 'app-home',
  imports: [CommonModule, CardComponent, IconDirective],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {
  private iconService = inject(IconService);
  private alertService = inject(AlertService);
  private medicalHistoryService = inject(MedicalHistoryService);
  private cdr = inject(ChangeDetectorRef);

  constructor() {
    this.iconService.addIcon(...[RiseOutline, FallOutline]);
  }

  loadingStats = true;

  AnalyticEcommerce = [
    { title: 'Medical History Status', amount: 'Unknown', background: 'bg-light-primary', border: 'border-primary', icon: 'rise', percentage: '0%', color: 'text-primary', number: 'Profile completeness', note: 'Availability of personal medical history' },
    { title: 'Recorded Surgeries', amount: '0', background: 'bg-light-success', border: 'border-success', icon: 'rise', percentage: '0%', color: 'text-success', number: 'Surgical events', note: 'Total surgeries in history' },
    { title: 'Pending Alerts', amount: '0', background: 'bg-light-warning', border: 'border-warning', icon: 'fall', percentage: '0%', color: 'text-warning', number: 'Unresolved notifications', note: 'Alerts requiring your attention' },
    { title: 'Critical Risk Alerts', amount: '0', background: 'bg-light-danger', border: 'border-danger', icon: 'fall', percentage: '0%', color: 'text-danger', number: 'Critical unresolved alerts', note: 'Highest urgency risks' }
  ];

  severityChart: ChartPoint[] = [];
  statusChart: ChartPoint[] = [];
  riskFactorChart: ChartPoint[] = [];
  latestRiskAlerts: AlertResponse[] = [];

  ngOnInit(): void {
    setTimeout(() => this.loadStatistics());
  }

  private loadStatistics(): void {
    this.loadingStats = true;

    forkJoin({
      medicalHistory: this.medicalHistoryService.getMyMedicalHistory().pipe(catchError(() => of(null as unknown as MedicalHistoryResponse))),
      alerts: this.alertService.getMyAlerts().pipe(catchError(() => of([] as AlertResponse[])))
    }).subscribe(({ medicalHistory, alerts }) => {
      setTimeout(() => {
        this.updatePatientStats(medicalHistory, alerts);
        this.loadingStats = false;
        this.cdr.detectChanges();
      });
    });
  }

  private updatePatientStats(medicalHistory: MedicalHistoryResponse | null, alerts: AlertResponse[]): void {
    const hasHistory = !!medicalHistory;
    const surgeriesCount = medicalHistory?.surgeries?.length || 0;

    const unresolvedAlerts = alerts.filter(alert => !alert.resolved);
    const pendingAlerts = unresolvedAlerts.length;
    const criticalAlerts = unresolvedAlerts.filter(alert => (alert.severity || '').toUpperCase() === 'CRITICAL').length;

    const pendingRate = this.toPercent(pendingAlerts, alerts.length);
    const criticalRate = this.toPercent(criticalAlerts, alerts.length);

    this.AnalyticEcommerce = [
      { title: 'Medical History Status', amount: hasHistory ? 'Available' : 'Missing', background: 'bg-light-primary', border: 'border-primary', icon: 'rise', percentage: hasHistory ? '100%' : '0%', color: 'text-primary', number: hasHistory ? 'Profile complete' : 'No record yet', note: 'Availability of personal medical history' },
      { title: 'Recorded Surgeries', amount: String(surgeriesCount), background: 'bg-light-success', border: 'border-success', icon: 'rise', percentage: hasHistory ? '100%' : '0%', color: 'text-success', number: 'Surgical events', note: 'Total surgeries in history' },
      { title: 'Pending Alerts', amount: String(pendingAlerts), background: 'bg-light-warning', border: 'border-warning', icon: 'fall', percentage: `${pendingRate}%`, color: 'text-warning', number: 'Unresolved notifications', note: 'Alerts requiring your attention' },
      { title: 'Critical Risk Alerts', amount: String(criticalAlerts), background: 'bg-light-danger', border: 'border-danger', icon: 'fall', percentage: `${criticalRate}%`, color: 'text-danger', number: 'Critical unresolved alerts', note: 'Highest urgency risks' }
    ];

    const criticalCount = alerts.filter(alert => (alert.severity || '').toUpperCase() === 'CRITICAL').length;
    const warningCount = alerts.filter(alert => (alert.severity || '').toUpperCase() === 'WARNING').length;
    const infoCount = alerts.filter(alert => (alert.severity || '').toUpperCase() === 'INFO').length;

    this.severityChart = this.buildChart([
      { label: 'Critical', value: criticalCount, colorClass: 'bg-danger' },
      { label: 'Warning', value: warningCount, colorClass: 'bg-warning' },
      { label: 'Info', value: infoCount, colorClass: 'bg-info' }
    ]);

    const resolvedCount = alerts.filter(alert => alert.resolved).length;
    this.statusChart = this.buildChart([
      { label: 'Pending', value: pendingAlerts, colorClass: 'bg-warning' },
      { label: 'Resolved', value: resolvedCount, colorClass: 'bg-success' }
    ]);

    const comorbiditiesCount = this.countItems(medicalHistory?.comorbidities);
    const medicationAllergiesCount = this.countItems(medicalHistory?.medicationAllergies);
    const foodAllergiesCount = this.countItems(medicalHistory?.foodAllergies);
    const environmentalAllergiesCount = this.countItems(medicalHistory?.environmentalAllergies);

    this.riskFactorChart = this.buildChart([
      { label: 'Comorbidities', value: comorbiditiesCount, colorClass: 'bg-primary' },
      { label: 'Medication Allergies', value: medicationAllergiesCount, colorClass: 'bg-info' },
      { label: 'Food Allergies', value: foodAllergiesCount, colorClass: 'bg-warning' },
      { label: 'Environmental Allergies', value: environmentalAllergiesCount, colorClass: 'bg-danger' }
    ]);

    this.latestRiskAlerts = unresolvedAlerts
      .sort((firstAlert, secondAlert) => {
        const severityDiff = this.severityWeight(secondAlert.severity) - this.severityWeight(firstAlert.severity);
        if (severityDiff !== 0) {
          return severityDiff;
        }
        return new Date(secondAlert.createdAt).getTime() - new Date(firstAlert.createdAt).getTime();
      })
      .slice(0, 5);
  }

  private countItems(value?: string): number {
    if (!value) {
      return 0;
    }
    return value
      .split(',')
      .map(item => item.trim())
      .filter(item => item.length > 0).length;
  }

  private buildChart(series: Array<{ label: string; value: number; colorClass: string }>): ChartPoint[] {
    const maxValue = Math.max(1, ...series.map(item => item.value));
    return series.map(item => ({
      ...item,
      percent: Math.round((item.value / maxValue) * 100)
    }));
  }

  private severityWeight(severity: string): number {
    const mappedSeverity: Record<string, number> = { CRITICAL: 3, WARNING: 2, INFO: 1 };
    return mappedSeverity[(severity || '').toUpperCase()] || 0;
  }

  private toPercent(value: number, total: number): number {
    return total > 0 ? Math.round((value / total) * 100) : 0;
  }
}