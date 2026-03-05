import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AlertService } from 'src/app/core/services/alert.service';
import { MedicalHistoryService } from 'src/app/core/services/medical-history.service';
import { AlertResponse } from 'src/app/core/models/alert.model';
import { MedicalHistoryResponse } from 'src/app/core/models/medical-history.model';
import { UserDto } from 'src/app/core/models/user.dto';

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
    { title: 'Assigned Patients', amount: '0', background: 'bg-light-primary', border: 'border-primary', icon: 'rise', percentage: '0%', color: 'text-primary', number: 'Under follow-up', note: 'Patients assigned to caregiver' },
    { title: 'Medical Histories Available', amount: '0', background: 'bg-light-success', border: 'border-success', icon: 'rise', percentage: '0%', color: 'text-success', number: 'Records accessible', note: 'Medical histories currently visible' },
    { title: 'Pending Risk Alerts', amount: '0', background: 'bg-light-warning', border: 'border-warning', icon: 'fall', percentage: '0%', color: 'text-warning', number: 'Unresolved alerts', note: 'Alerts requiring caregiver action' },
    { title: 'Critical Risk Alerts', amount: '0', background: 'bg-light-danger', border: 'border-danger', icon: 'fall', percentage: '0%', color: 'text-danger', number: 'Critical unresolved alerts', note: 'Highest urgency patient risks' }
  ];

  severityChart: ChartPoint[] = [];
  statusChart: ChartPoint[] = [];
  progressionChart: ChartPoint[] = [];
  latestRiskAlerts: AlertResponse[] = [];

  ngOnInit(): void {
    setTimeout(() => this.loadStatistics());
  }

  private loadStatistics(): void {
    this.loadingStats = true;

    forkJoin({
      assignedPatients: this.medicalHistoryService.getAssignedPatients().pipe(catchError(() => of([] as UserDto[]))),
      alerts: this.alertService.getCaregiverAlerts().pipe(catchError(() => of([] as AlertResponse[])))
    }).subscribe(({ assignedPatients, alerts }) => {
      const historyRequests = assignedPatients.map(patient =>
        this.medicalHistoryService.getPatientHistoryForCaregiver(patient.id)
          .pipe(catchError(() => of(null as unknown as MedicalHistoryResponse)))
      );

      if (historyRequests.length === 0) {
        setTimeout(() => {
          this.updateCaregiverStats(assignedPatients, [], alerts);
          this.loadingStats = false;
          this.cdr.detectChanges();
        });
        return;
      }

      forkJoin(historyRequests).subscribe((histories) => {
        setTimeout(() => {
          const validHistories = histories.filter((history): history is MedicalHistoryResponse => history !== null);
          this.updateCaregiverStats(assignedPatients, validHistories, alerts);
          this.loadingStats = false;
          this.cdr.detectChanges();
        });
      });
    });
  }

  private updateCaregiverStats(
    assignedPatients: UserDto[],
    histories: MedicalHistoryResponse[],
    alerts: AlertResponse[]
  ): void {
    const assignedCount = assignedPatients.length;
    const historyCount = histories.length;

    const unresolvedAlerts = alerts.filter(alert => !alert.resolved);
    const pendingAlerts = unresolvedAlerts.length;
    const criticalAlerts = unresolvedAlerts.filter(alert => (alert.severity || '').toUpperCase() === 'CRITICAL').length;

    const historyCoverage = this.toPercent(historyCount, assignedCount);
    const pendingRate = this.toPercent(pendingAlerts, alerts.length);
    const criticalRate = this.toPercent(criticalAlerts, alerts.length);

    this.AnalyticEcommerce = [
      { title: 'Assigned Patients', amount: String(assignedCount), background: 'bg-light-primary', border: 'border-primary', icon: 'rise', percentage: '100%', color: 'text-primary', number: 'Under follow-up', note: 'Patients assigned to caregiver' },
      { title: 'Medical Histories Available', amount: String(historyCount), background: 'bg-light-success', border: 'border-success', icon: 'rise', percentage: `${historyCoverage}%`, color: 'text-success', number: 'Coverage of assigned patients', note: 'Medical histories currently visible' },
      { title: 'Pending Risk Alerts', amount: String(pendingAlerts), background: 'bg-light-warning', border: 'border-warning', icon: 'fall', percentage: `${pendingRate}%`, color: 'text-warning', number: 'Unresolved alerts', note: 'Alerts requiring caregiver action' },
      { title: 'Critical Risk Alerts', amount: String(criticalAlerts), background: 'bg-light-danger', border: 'border-danger', icon: 'fall', percentage: `${criticalRate}%`, color: 'text-danger', number: 'Critical unresolved alerts', note: 'Highest urgency patient risks' }
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

    const mildCount = histories.filter(history => (history.progressionStage || '').toUpperCase() === 'MILD').length;
    const moderateCount = histories.filter(history => (history.progressionStage || '').toUpperCase() === 'MODERATE').length;
    const severeCount = histories.filter(history => (history.progressionStage || '').toUpperCase() === 'SEVERE').length;
    this.progressionChart = this.buildChart([
      { label: 'Mild', value: mildCount, colorClass: 'bg-primary' },
      { label: 'Moderate', value: moderateCount, colorClass: 'bg-warning' },
      { label: 'Severe', value: severeCount, colorClass: 'bg-danger' }
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