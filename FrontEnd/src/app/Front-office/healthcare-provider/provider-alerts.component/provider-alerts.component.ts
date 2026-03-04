import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AlertService } from '../../../core/services/alert.service';
import { MedicalHistoryService } from '../../../core/services/medical-history.service'; // to get patients list
import { AlertResponse, AlertRequest } from '../../../core/models/alert.model';
import { UserDto } from '../../../core/models/user.dto';

@Component({
  selector: 'app-provider-alerts',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './provider-alerts.component.html',
  styleUrls: ['./provider-alerts.component.scss']
})
export class ProviderAlertsComponent implements OnInit {
  alerts: AlertResponse[] = [];
  patients: UserDto[] = [];
  selectedPatientId: number | null = null;
  loading = false;
  error: string | null = null;
  successMessage: string | null = null;

  // Form for creating/updating alert
  alertForm: FormGroup;
  editingAlertId: number | null = null;
  showForm = false;

  constructor(
    private alertService: AlertService,
    private medicalHistoryService: MedicalHistoryService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.alertForm = this.fb.group({
      patientId: ['', Validators.required],
      message: ['', [Validators.required, Validators.maxLength(500)]],
      severity: ['INFO', Validators.required]
    });
  }

  ngOnInit(): void {
    // Defer initial data loading to avoid ExpressionChangedAfterItHasBeenCheckedError
    setTimeout(() => {
      this.loadPatients();
      this.loadAllAlerts();
    });
  }

  loadPatients(): void {
    this.medicalHistoryService.getPatients().subscribe({
      next: (data) => {
        this.patients = data;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = 'Failed to load patients: ' + err.message;
        this.cdr.detectChanges();
      }
    });
  }

  loadAllAlerts(): void {
    this.loading = true;
    this.error = null;
    // Since provider endpoint to get all alerts doesn't exist, we'll load by selected patient or just load all? 
    // For simplicity, we'll initially load all alerts by iterating patients? That's inefficient.
    // Better: provider can view alerts by selecting a patient from dropdown.
    // So we'll not load all initially; instead we'll load when a patient is selected.
    if (this.selectedPatientId) {
      this.loadAlertsForPatient(this.selectedPatientId);
    } else {
      this.alerts = [];
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  onPatientSelect(patientId: number): void {
    this.selectedPatientId = patientId;
    this.loadAlertsForPatient(patientId);
  }

  loadAlertsForPatient(patientId: number): void {
    this.loading = true;
    this.cdr.detectChanges();
    this.alertService.getAlertsByPatient(patientId).subscribe({
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

  // Trigger generation
  triggerGeneration(): void {
    this.alertService.triggerAlertGeneration().subscribe({
      next: (response) => {
        this.successMessage = 'Alert generation triggered successfully.';
        this.cdr.detectChanges();
        if (this.selectedPatientId) {
          this.loadAlertsForPatient(this.selectedPatientId);
        }
        setTimeout(() => {
          this.successMessage = null;
          this.cdr.detectChanges();
        }, 3000);
      },
      error: (err) => {
        this.error = err.message;
        this.cdr.detectChanges();
      }
    });
  }

  // Show create form
  newAlert(): void {
    this.editingAlertId = null;
    this.alertForm.reset({ severity: 'INFO' });
    if (this.selectedPatientId) {
      this.alertForm.patchValue({ patientId: this.selectedPatientId });
    }
    this.showForm = true;
  }

  // Edit alert
  editAlert(alert: AlertResponse): void {
    this.editingAlertId = alert.id;
    this.alertForm.patchValue({
      patientId: alert.patientId,
      message: alert.message,
      severity: alert.severity
    });
    this.showForm = true;
  }

  // Cancel form
  cancelForm(): void {
    this.showForm = false;
    this.editingAlertId = null;
    this.alertForm.reset();
  }

  // Submit form (create or update)
  onSubmit(): void {
    if (this.alertForm.invalid) return;
    const request: AlertRequest = this.alertForm.value;
    if (this.editingAlertId) {
      // Update
      this.alertService.updateAlert(this.editingAlertId, request).subscribe({
        next: (updated) => {
          this.successMessage = 'Alert updated.';
          this.cancelForm();
          this.cdr.detectChanges();
          if (this.selectedPatientId) {
            this.loadAlertsForPatient(this.selectedPatientId);
          }
          setTimeout(() => {
            this.successMessage = null;
            this.cdr.detectChanges();
          }, 3000);
        },
        error: (err) => {
          this.error = err.message;
          this.cdr.detectChanges();
        }
      });
    } else {
      // Create
      this.alertService.createAlert(request).subscribe({
        next: (created) => {
          this.successMessage = 'Alert created.';
          this.cancelForm();
          this.cdr.detectChanges();
          if (this.selectedPatientId === created.patientId) {
            this.alerts = [...this.alerts, created];
          } else if (this.selectedPatientId) {
            this.loadAlertsForPatient(this.selectedPatientId);
          }
          setTimeout(() => {
            this.successMessage = null;
            this.cdr.detectChanges();
          }, 3000);
        },
        error: (err) => {
          this.error = err.message;
          this.cdr.detectChanges();
        }
      });
    }
  }

  // Resolve alert
  resolveAlert(alertId: number): void {
    if (!confirm('Mark this alert as resolved?')) return;
    this.alertService.resolveAlert(alertId).subscribe({
      next: () => {
        this.successMessage = 'Alert resolved.';
        this.cdr.detectChanges();
        if (this.selectedPatientId) {
          this.loadAlertsForPatient(this.selectedPatientId);
        }
        setTimeout(() => {
          this.successMessage = null;
          this.cdr.detectChanges();
        }, 3000);
      },
      error: (err) => {
        this.error = err.message;
        this.cdr.detectChanges();
      }
    });
  }

  // Delete alert
  deleteAlert(alertId: number): void {
    if (!confirm('Are you sure you want to delete this alert?')) return;
    this.alertService.deleteAlert(alertId).subscribe({
      next: () => {
        this.successMessage = 'Alert deleted.';
        this.alerts = this.alerts.filter(a => a.id !== alertId);
        this.cdr.detectChanges();
        setTimeout(() => {
          this.successMessage = null;
          this.cdr.detectChanges();
        }, 3000);
      },
      error: (err) => {
        this.error = err.message;
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