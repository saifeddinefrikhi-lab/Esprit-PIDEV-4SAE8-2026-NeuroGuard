import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AlertResponse, AlertRequest } from '../../core/models/alert.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  private apiUrl = environment.apiUrl; // points to gateway

  constructor(private http: HttpClient, private authService: AuthService) {}

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
  // Trigger rule‑based alert generation for all patients
  triggerAlertGeneration(): Observable<string> {
    return this.http.post(`${this.apiUrl}/api/provider/alerts/generate`, {}, { responseType: 'text' })
      .pipe(catchError(err => this.handleError(err)));
  }

  // Trigger ML‑based predictive alert generation for all patients
  triggerPredictiveGeneration(): Observable<string> {
    return this.http.post(`${this.apiUrl}/api/provider/alerts/generate-predictive`, {}, { responseType: 'text' })
      .pipe(catchError(err => this.handleError(err)));
  }

  // Create a custom alert
  createAlert(request: AlertRequest): Observable<AlertResponse> {
    console.log(`[AlertService] Creating alert at: ${this.apiUrl}/api/provider/alerts`, request);
    return this.http.post<AlertResponse>(`${this.apiUrl}/api/provider/alerts`, request)
      .pipe(catchError(err => {
        console.error('[AlertService] Create alert error:', err);
        return this.handleError(err);
      }));
  }

  // Update an alert
  updateAlert(alertId: number, request: AlertRequest): Observable<AlertResponse> {
    console.log(`[AlertService] Updating alert ${alertId} at: ${this.apiUrl}/api/provider/alerts/${alertId}`, request);
    return this.http.put<AlertResponse>(`${this.apiUrl}/api/provider/alerts/${alertId}`, request)
      .pipe(catchError(err => {
        console.error(`[AlertService] Update alert ${alertId} error:`, err);
        return this.handleError(err);
      }));
  }

  // Resolve an alert
  resolveAlert(alertId: number): Observable<AlertResponse> {
       const userRole = this.authService.currentUser?.role?.toUpperCase() || 'PROVIDER';
       let endpoint: string;

       switch (userRole) {
         case 'CAREGIVER':
           endpoint = `${this.apiUrl}/api/caregiver/alerts/${alertId}/resolve`;
           break;
         case 'PATIENT':
           endpoint = `${this.apiUrl}/api/patient/alerts/${alertId}/resolve`;
           break;
         case 'PROVIDER':
         default:
           endpoint = `${this.apiUrl}/api/provider/alerts/${alertId}/resolve`;
           break;
       }

       console.log(`[AlertService] Resolving alert ${alertId} at: ${endpoint}`);
       return this.http.patch<AlertResponse>(endpoint, {})
         .pipe(catchError(err => {
           console.error(`[AlertService] Resolve alert ${alertId} error:`, err);
           return this.handleError(err);
         }));
     }

  // Delete an alert
  deleteAlert(alertId: number): Observable<void> {
    console.log(`[AlertService] Deleting alert ${alertId} from: ${this.apiUrl}/api/provider/alerts/${alertId}`);
    return this.http.delete<void>(`${this.apiUrl}/api/provider/alerts/${alertId}`)
      .pipe(
        catchError(err => {
          console.error(`[AlertService] Delete failed for alert ${alertId}:`, err);
          return this.handleError(err);
        })
      );
  }

  // Get alerts for a specific patient (provider view)
  getAlertsByPatient(patientId: number): Observable<AlertResponse[]> {
    console.log(`[AlertService] Fetching alerts for patient ${patientId} from: ${this.apiUrl}/api/provider/alerts/patient/${patientId}`);
    return this.http.get<AlertResponse[]>(`${this.apiUrl}/api/provider/alerts/patient/${patientId}`)
      .pipe(catchError(err => {
        console.error(`[AlertService] Get alerts for patient ${patientId} error:`, err);
        return this.handleError(err);
      }));
  }
}