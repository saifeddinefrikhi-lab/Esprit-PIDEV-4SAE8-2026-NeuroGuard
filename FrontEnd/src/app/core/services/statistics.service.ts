import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PatientStatistics {
  patientId: number;
  patientName: string;
  totalAssurances: number;
  
  // Risk Metrics
  averageAlzheimersRisk: number;
  highestAlzheimersRisk: number;
  lowestAlzheimersRisk: number;
  standardDeviationRisk: number;
  
  // Cost Metrics
  totalEstimatedCost: number;
  averageAnnualCost: number;
  medianAnnualCost: number;
  
  // Alert Metrics
  totalActiveAlerts: number;
  averageAlertsPerAssurance: number;
  highestSeverityAlerts: string[];
  
  // Complexity Metrics
  averageComplexityScore: number;
  maxComplexityScore: number;
  
  // Procedure Analysis
  recommendedProceduresFrequency: Map<string, number>;
  
  // Health Profile
  careTeamAverageSize: number;
  patientsNeedingNeurology: number;
  patientsNeedingGeriatrics: number;
  
  // Trend Analysis
  overallRiskLevel: string;
}

export interface AssuranceStatistics {
  assuranceId: number;
  assuranceType: string;
  
  // Patient Demographics
  totalPatientsEnrolled: number;
  averagePatientAge: number;
  
  // Risk Metrics
  averageRiskScore: number;
  riskDistribution: number;
  patientsHighRisk: number;
  patientsMediumRisk: number;
  patientsLowRisk: number;
  
  // Cost Metrics
  totalProjectedCost: number;
  averageClaimCost: number;
  costVariance: number;
  costStandardDeviation: number;
  
  // Alzheimer-specific
  averageAlzheimersPrevalence: number;
  patientsWithHighAlzRisk: number;
  
  // Procedure Analytics
  topRecommendedProcedures: ProcedureStatistic[];
  totalUniqueProcedures: number;
  
  // Care Coordination
  averageCareTeamSize: number;
  neurology_referralsNeeded: number;
  geriatricsReferralsNeeded: number;
  
  // Comparative Analysis
  comparisonToNational: number;
  performanceRating: string;
}

export interface ProcedureStatistic {
  procedureName: string;
  frequency: number;
  percentageOfAssurances: number;
  averageCostPerProcedure: number;
}

@Injectable({
  providedIn: 'root'
})
export class StatisticsService {
  private apiUrl = `${environment.apiUrl}/api/assurances`;

  constructor(private http: HttpClient) {}

  /**
   * Get patient-level statistics
   */
  getPatientStatistics(patientId: number): Observable<PatientStatistics> {
    return this.http.get<PatientStatistics>(
      `${this.apiUrl}/stats/patient/${patientId}`
    );
  }

  /**
   * Get assurance-level statistics
   */
  getAssuranceStatistics(assuranceId: number): Observable<AssuranceStatistics> {
    return this.http.get<AssuranceStatistics>(
      `${this.apiUrl}/stats/assurance/${assuranceId}`
    );
  }
}
