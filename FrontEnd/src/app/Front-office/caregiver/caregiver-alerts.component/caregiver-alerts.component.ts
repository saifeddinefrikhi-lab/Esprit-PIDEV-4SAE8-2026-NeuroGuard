import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlertService } from '../../../core/services/alert.service';
import { AlertResponse } from '../../../core/models/alert.model';

@Component({
  selector: 'app-caregiver-alerts',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './caregiver-alerts.component.html',
  styleUrls: ['./caregiver-alerts.component.scss']
})
export class CaregiverAlertsComponent implements OnInit {
  alerts: AlertResponse[] = [];
  loading = false;
  error: string | null = null;

  constructor(
    private alertService: AlertService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    setTimeout(() => this.loadAlerts());
  }

  loadAlerts(): void {
    this.loading = true;
    this.error = null;
    this.cdr.detectChanges();
    this.alertService.getCaregiverAlerts().subscribe({
      next: (data) => {
        this.alerts = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err.message || 'Failed to load alerts';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  getSeverityClass(severity: string): string {
    switch (severity?.toUpperCase()) {
      case 'CRITICAL': return 'bg-danger text-white';
      case 'WARNING': return 'bg-warning text-dark';
      case 'INFO': return 'bg-info text-white';
      default: return 'bg-secondary text-white';
    }
  }

  getSeverityIcon(severity: string): string {
    switch (severity?.toUpperCase()) {
      case 'CRITICAL': return 'ti-alert-triangle';
      case 'WARNING': return 'ti-alert-circle';
      case 'INFO': return 'ti-info-circle';
      default: return 'ti-bell';
    }
  }
}