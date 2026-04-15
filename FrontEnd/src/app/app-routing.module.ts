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
        path: 'admin/consultations',
        loadComponent: () => import('./Back-office/pages/admin-consultations/admin-consultations.component').then((c) => c.AdminConsultationsComponent)
      },
      {
        path: 'admin/assurance',
        loadComponent: () => import('./Back-office/pages/assurance-admin/assurance-admin.component').then((c) => c.AssuranceAdminComponent)
      },
      // MOVED TO PATIENT: assurance/optimizer is now in patient/assurance management
      // {
      //   path: 'admin/assurance/optimizer',
      //   loadComponent: () => import('./Back-office/pages/assurance-admin/assurance-optimizer/assurance-optimizer.component').then((c) => c.AssuranceOptimizerComponent)
      // },
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
        path: 'patient/consultation',
        loadComponent: () => import('./Front-office/patient/patient-consultations/patient-consultations.component').then((c) => c.PatientConsultationsComponent)
      },
      {
        path: 'patient/assurance',
        loadComponent: () => import('./Front-office/patient/assurance-management/assurance-management.component').then((c) => c.AssuranceManagementComponent)
      },
      {
        path: 'patient/find-nearby-doctors',
        loadComponent: () => import('./pages/consultation/find-nearby-doctors/find-nearby-doctors.component').then((c) => c.FindNearbyDoctorsComponent)
      },
      {
        path: 'patient/reservations',
        loadComponent: () => import('./Front-office/patient/patient-reservations/patient-reservations.component').then((c) => c.PatientReservationsComponent)
      },
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
        path: 'caregiver/consultations',
        loadComponent: () => import('./Front-office/caregiver/caregiver-consultations/caregiver-consultations.component').then((c) => c.CaregiverConsultationsComponent)
      },
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
        path: 'provider/consultations',
        loadComponent: () => import('./Front-office/healthcare-provider/provider-consultations.component/provider-consultations.component').then((c) => c.ProviderConsultationsComponent)
      },
      {
        path: 'provider/consultations/history',
        loadComponent: () => import('./Front-office/healthcare-provider/provider-consultation-history/provider-consultation-history.component').then((c) => c.ProviderConsultationHistoryComponent)
      },
      {
        path: 'provider/availability',
        loadComponent: () => import('./Front-office/healthcare-provider/provider-availability/provider-availability.component').then((c) => c.ProviderAvailabilityComponent)
      },
      {
        path: 'provider/find-nearby-doctors',
        loadComponent: () => import('./pages/consultation/find-nearby-doctors/find-nearby-doctors.component').then((c) => c.FindNearbyDoctorsComponent)
      },
      {
        path: 'provider/reservations',
        loadComponent: () => import('./Front-office/healthcare-provider/provider-reservations/provider-reservations.component').then((c) => c.ProviderReservationsComponent)
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
export class AppRoutingModule {}