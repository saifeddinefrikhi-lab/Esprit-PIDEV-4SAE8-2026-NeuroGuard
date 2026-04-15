import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

// Project import
import { AdminLayout } from './theme/layouts/admin-layout/admin-layout.component';
import { GuestLayoutComponent } from './theme/layouts/guest-layout/guest-layout.component';
import { PatientLayout } from './theme/layouts/patient-layout/patient-layout.component';
import { CaregiverLayout } from './theme/layouts/caregiver-layout/caregiver-layout.component';
import { ProviderLayout } from './theme/layouts/provider-layout/provider-layout.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'homePage',
    pathMatch: 'full'
  },
  {
    path: '',
    component: AdminLayout,
    canActivate: [authGuard],
    data: { roles: ['ADMIN'] },
    children: [
      {
        path: 'admin/dashboard',
        loadComponent: () => import('./Back-office/dashboard/default/default.component').then((c) => c.DefaultComponent)
      },
      {
        path: 'admin/providers',
        loadComponent: () => import('./Back-office/pages/healthcare-privider-management/healthcare-privider-management').then((c) => c.HealthcarePrividerManagement)
      },
      {
        path: 'admin/caregivers',
        loadComponent: () => import('./Back-office/pages/caregiver-management/caregiver-management').then((c) => c.CaregiverManagement)
      },
      {
        path: 'admin/patients',
        loadComponent: () => import('./Back-office/pages/patient-management/patient-management').then((c) => c.PatientManagement)
      },
      {
        path: 'admin/care-plans',
        loadComponent: () => import('./Front-office/healthcare-provider/care-plan/care-plan-list/care-plan-list.component').then((c) => c.CarePlanListComponent)
      },
      {
        path: 'admin/care-plans/stats',
        loadComponent: () => import('./Back-office/pages/care-plan-stats/care-plan-stats.component').then((c) => c.CarePlanStatsComponent)
      },
      {
        path: 'admin/care-plans/new',
        loadComponent: () => import('./Front-office/healthcare-provider/care-plan/care-plan-form/care-plan-form.component').then((c) => c.CarePlanFormComponent)
      },
      {
        path: 'admin/care-plans/edit/:id',
        loadComponent: () => import('./Front-office/healthcare-provider/care-plan/care-plan-form/care-plan-form.component').then((c) => c.CarePlanFormComponent)
      },
      {
        path: 'admin/care-plans/view/:id',
        loadComponent: () => import('./Front-office/healthcare-provider/care-plan/care-plan-detail/care-plan-detail.component').then((c) => c.CarePlanDetailComponent)
      },
      {
        path: 'admin/prescriptions',
        loadComponent: () => import('./Front-office/healthcare-provider/prescription/prescription-list/prescription-list.component').then((c) => c.PrescriptionListComponent)
      },
      {
        path: 'admin/prescriptions/new',
        loadComponent: () => import('./Front-office/healthcare-provider/prescription/prescription-form/prescription-form.component').then((c) => c.PrescriptionFormComponent)
      },
      {
        path: 'admin/prescriptions/edit/:id',
        loadComponent: () => import('./Front-office/healthcare-provider/prescription/prescription-form/prescription-form.component').then((c) => c.PrescriptionFormComponent)
      },
      {
        path: 'admin/prescriptions/view/:id',
        loadComponent: () => import('./Front-office/healthcare-provider/prescription/prescription-detail/prescription-detail.component').then((c) => c.PrescriptionDetailComponent)
      },
      {
        path: 'admin/prescription-analytics',
        loadComponent: () => import('./Back-office/pages/prescription-analytics/prescription-analytics.component').then((c) => c.PrescriptionAnalyticsComponent)
      },
      {
        path: 'admin/medications',
        loadComponent: () => import('./Back-office/pages/pharmacy-clinic-management/pharmacy-clinic-management.component').then((c) => c.PharmacyClinicManagementComponent)
      },
      {
        path: 'admin/risk-analysis',
        loadComponent: () => import('./Back-office/dashboard/risk-analysis/risk-analysis.component').then((c) => c.RiskAnalysisComponent)
        },
        {
          path: 'admin/pharmacies-clinics',
          loadComponent: () => import('./Back-office/pages/pharmacy-clinic-management/pharmacy-clinic-management.component').then((c) => c.PharmacyClinicManagementComponent)
        }
    ]
  },

  {
    path: '',
    component: PatientLayout,
    canActivate: [authGuard],
    data: { roles: ['PATIENT'] },
    children: [
      {
        path: 'patient/home',
        loadComponent: () => import('./Front-office/patient/home/home.component').then((c) => c.HomeComponent)
      },
      {
        path: 'patient/medical-history',
        loadComponent: () => import('./Front-office/patient/patient-medical-history/patient-medical-history').then((c) => c.PatientMedicalHistoryComponent)
      },
      {
        path: 'patient/care-plans',
        loadComponent: () => import('./Front-office/patient/care-plan-kanban/care-plan-kanban.component').then((c) => c.CarePlanKanbanComponent)
      },
      {
        path: 'patient/care-plans/view/:id',
        loadComponent: () => import('./Front-office/healthcare-provider/care-plan/care-plan-detail/care-plan-detail.component').then((c) => c.CarePlanDetailComponent)
      },
      {
        path: 'patient/prescriptions',
        loadComponent: () => import('./Front-office/patient/prescription/patient-prescription-list/patient-prescription-list.component').then((c) => c.PatientPrescriptionListComponent)
      },
      {
        path: 'patient/prescriptions/view/:id',
        loadComponent: () => import('./Front-office/healthcare-provider/prescription/prescription-detail/prescription-detail.component').then((c) => c.PrescriptionDetailComponent)
      },
      {
        path: 'patient/pharmacy',
        loadComponent: () => import('./Front-office/patient/pharmacy/pharmacy-locator/pharmacy-locator.component').then((c) => c.PharmacyLocatorComponent)
      },
      {
        path: 'patient/medication',
        redirectTo: 'patient/prescriptions',
        pathMatch: 'full'
      }
    ]
  },

  {
    path: '',
    component: CaregiverLayout,
    canActivate: [authGuard],
    data: { roles: ['CAREGIVER'] },
    children: [
      {
        path: 'caregiver/home',
        loadComponent: () => import('./Front-office/caregiver/home/home.component').then((c) => c.HomeComponent)
      },
      {
        path: 'caregiver/medical-history/patients',
        loadComponent: () => import('./Front-office/caregiver/caregiver-patient-list/caregiver-patient-list').then((c) => c.CaregiverPatientListComponent)
      },
      {
        path: 'caregiver/medical-history/view/:patientId',
        loadComponent: () => import('./Front-office/caregiver/caregiver-patient-detail/caregiver-patient-detail').then((c) => c.CaregiverPatientDetailComponent)
      },
      {
        path: 'caregiver/care-plans',
        loadComponent: () => import('./Front-office/healthcare-provider/care-plan/care-plan-list/care-plan-list.component').then((c) => c.CarePlanListComponent)
      },
      {
        path: 'caregiver/care-plans/view/:id',
        loadComponent: () => import('./Front-office/healthcare-provider/care-plan/care-plan-detail/care-plan-detail.component').then((c) => c.CarePlanDetailComponent)
      },
      {
        path: 'caregiver/prescriptions',
        loadComponent: () => import('./Front-office/healthcare-provider/prescription/prescription-list/prescription-list.component').then((c) => c.PrescriptionListComponent)
      },
      {
        path: 'caregiver/prescriptions/view/:id',
        loadComponent: () => import('./Front-office/healthcare-provider/prescription/prescription-detail/prescription-detail.component').then((c) => c.PrescriptionDetailComponent)
      }
    ]
  },

  {
    path: '',
    component: ProviderLayout,
    canActivate: [authGuard],
    data: { roles: ['PROVIDER'] },
    children: [
      {
        path: 'provider/home',
        loadComponent: () => import('./Front-office/healthcare-provider/home/home.component').then((c) => c.HomeComponent)
      },
      {
        path: 'provider/medical-history',
        loadComponent: () => import('./Front-office/healthcare-provider/provider-medical-history-list/provider-medical-history-list').then((c) => c.ProviderMedicalHistoryListComponent)
      },

      {
        path: 'provider/medical-history/new',
        loadComponent: () => import('./Front-office/healthcare-provider/provider-medical-history-form/provider-medical-history-form').then((c) => c.ProviderMedicalHistoryFormComponent)
      },
      {
        path: 'provider/medical-history/edit/:patientId',
        loadComponent: () => import('./Front-office/healthcare-provider/provider-medical-history-form/provider-medical-history-form').then((c) => c.ProviderMedicalHistoryFormComponent)
      },
      {
        path: 'provider/medical-history/view/:patientId',
        loadComponent: () => import('./Front-office/healthcare-provider/provider-medical-history-detail/provider-medical-history-detail').then((c) => c.ProviderMedicalHistoryDetailComponent)
      },
      {
        path: 'provider/care-plans',
        loadComponent: () => import('./Front-office/healthcare-provider/care-plan/care-plan-list/care-plan-list.component').then((c) => c.CarePlanListComponent)
      },
      {
        path: 'provider/care-plans/new',
        loadComponent: () => import('./Front-office/healthcare-provider/care-plan/care-plan-form/care-plan-form.component').then((c) => c.CarePlanFormComponent)
      },
      {
        path: 'provider/care-plans/edit/:id',
        loadComponent: () => import('./Front-office/healthcare-provider/care-plan/care-plan-form/care-plan-form.component').then((c) => c.CarePlanFormComponent)
      },
      {
        path: 'provider/care-plans/view/:id',
        loadComponent: () => import('./Front-office/healthcare-provider/care-plan/care-plan-detail/care-plan-detail.component').then((c) => c.CarePlanDetailComponent)
      },
      {
        path: 'provider/prescriptions',
        loadComponent: () => import('./Front-office/healthcare-provider/prescription/prescription-list/prescription-list.component').then((c) => c.PrescriptionListComponent)
      },
      {
        path: 'provider/prescriptions/new',
        loadComponent: () => import('./Front-office/healthcare-provider/prescription/prescription-form/prescription-form.component').then((c) => c.PrescriptionFormComponent)
      },
      {
        path: 'provider/prescriptions/edit/:id',
        loadComponent: () => import('./Front-office/healthcare-provider/prescription/prescription-form/prescription-form.component').then((c) => c.PrescriptionFormComponent)
      },
      {
        path: 'provider/prescriptions/view/:id',
        loadComponent: () => import('./Front-office/healthcare-provider/prescription/prescription-detail/prescription-detail.component').then((c) => c.PrescriptionDetailComponent)
      }
    ]
  },

  {
    path: '',
    component: GuestLayoutComponent,
    children: [
      {
        path: 'login',
        loadComponent: () => import('./pages/authentication/auth-login/auth-login.component').then((c) => c.AuthLoginComponent)
      },
      {
        path: 'register',
        loadComponent: () =>
          import('./pages/authentication/auth-register/auth-register.component').then((c) => c.AuthRegisterComponent)
      },
      {
        path: 'homePage',
        loadComponent: () => import('./Front-office/home-page/home-page.component').then((c) => c.HomePageComponent)
      },
      {
        path: 'restricted',
        loadComponent: () => import('./pages/restriction/restricted.component').then((c) => c.RestrictedComponent)
      }
    ]
  },

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }