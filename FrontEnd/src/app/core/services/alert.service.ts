import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AlertResponse, AlertRequest } from '../../core/models/alert.model';

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  private apiUrl = environment.apiUrl; // points to gateway

  constructor(private http: HttpClient) {}

  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'An error occurred';
    if (error.error instanceof ErrorEvent) {
      errorMessage = `Client Error: ${error.error.message}`;
    } else {
      errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
    }
    console.error('[AlertService]', errorMessage);
    return throwError(() => new Error(errorMessage));
  }

  // ================== PATIENT ENDPOINTS ==================
  getMyAlerts(): Observable<AlertResponse[]> {
    return this.http.get<AlertResponse[]>(`${this.apiUrl}/api/patient/alerts`)
      .pipe(catchError(err => this.handleError(err)));
  }

  // ================== CAREGIVER ENDPOINTS ==================
  getCaregiverAlerts(): Observable<AlertResponse[]> {
    return this.http.get<AlertResponse[]>(`${this.apiUrl}/api/caregiver/alerts`)
      .pipe(catchError(err => this.handleError(err)));
  }

  // ================== PROVIDER ENDPOINTS ==================
  // Trigger automatic alert generation for all patients
  triggerAlertGeneration(): Observable<string> {
    return this.http.post(`${this.apiUrl}/api/provider/alerts/generate`, {}, { responseType: 'text' })
      .pipe(catchError(err => this.handleError(err)));
  }

  // Create a custom alert
  createAlert(request: AlertRequest): Observable<AlertResponse> {
    return this.http.post<AlertResponse>(`${this.apiUrl}/api/provider/alerts`, request)
      .pipe(catchError(err => this.handleError(err)));
  }

  // Update an alert
  updateAlert(alertId: number, request: AlertRequest): Observable<AlertResponse> {
    return this.http.put<AlertResponse>(`${this.apiUrl}/api/provider/alerts/${alertId}`, request)
      .pipe(catchError(err => this.handleError(err)));
  }

  // Resolve an alert
  resolveAlert(alertId: number): Observable<AlertResponse> {
    return this.http.patch<AlertResponse>(`${this.apiUrl}/api/provider/alerts/${alertId}/resolve`, {})
      .pipe(catchError(err => this.handleError(err)));
  }

  // Delete an alert
  deleteAlert(alertId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/provider/alerts/${alertId}`)
      .pipe(catchError(err => this.handleError(err)));
  }

  // Get alerts for a specific patient (provider view)
  getAlertsByPatient(patientId: number): Observable<AlertResponse[]> {
    return this.http.get<AlertResponse[]>(`${this.apiUrl}/api/provider/alerts/patient/${patientId}`)
      .pipe(catchError(err => this.handleError(err)));
  }
}