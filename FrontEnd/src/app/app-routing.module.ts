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
        path: 'admin/users',
        loadComponent: () => import('./Back-office/pages/user-list.component/user-list.component').then((c) => c.UserListComponent)
      },
    
      {
        path: 'admin/forum',
        loadComponent: () => import('./pages/post-list.component/post-list.component').then((c) => c.PostListComponent)
      },
      {
        path: 'admin/forum/new',
        loadComponent: () => import('./pages/post-form.component/post-form.component').then((c) => c.PostFormComponent)
      },
      {
        path: 'admin/forum/edit/:id',
        loadComponent: () => import('./pages/post-form.component/post-form.component').then((c) => c.PostFormComponent)
      },
      {
        path: 'admin/forum/:id',
        loadComponent: () => import('./pages/post-detail.component/post-detail.component').then((c) => c.PostDetailComponent)
      },
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
        path: 'patient/alerts',
        loadComponent: () => import('./Front-office/patient/patient-alerts.component/patient-alerts.component').then((c) => c.PatientAlertsComponent)
      },

       {
        path: 'patient/forum',
        loadComponent: () => import('./pages/post-list.component/post-list.component').then((c) => c.PostListComponent)
      },
      {
        path: 'patient/forum/new',
        loadComponent: () => import('./pages/post-form.component/post-form.component').then((c) => c.PostFormComponent)
      },
      {
        path: 'patient/forum/edit/:id',
        loadComponent: () => import('./pages/post-form.component/post-form.component').then((c) => c.PostFormComponent)
      },
      {
        path: 'patient/forum/:id',
        loadComponent: () => import('./pages/post-detail.component/post-detail.component').then((c) => c.PostDetailComponent)
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
        path: 'caregiver/alerts',
        loadComponent: () => import('./Front-office/caregiver/caregiver-alerts.component/caregiver-alerts.component').then((c) => c.CaregiverAlertsComponent)
      },

      {
        path: 'caregiver/forum',
        loadComponent: () => import('./pages/post-list.component/post-list.component').then((c) => c.PostListComponent)
      },
      {
        path: 'caregiver/forum/new',
        loadComponent: () => import('./pages/post-form.component/post-form.component').then((c) => c.PostFormComponent)
      },
      {
        path: 'caregiver/forum/edit/:id',
        loadComponent: () => import('./pages/post-form.component/post-form.component').then((c) => c.PostFormComponent)
      },
      {
        path: 'caregiver/forum/:id',
        loadComponent: () => import('./pages/post-detail.component/post-detail.component').then((c) => c.PostDetailComponent)
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
        path: 'provider/alerts',
        loadComponent: () => import('./Front-office/healthcare-provider/provider-alerts.component/provider-alerts.component').then((c) => c.ProviderAlertsComponent)
      },

      {
        path: 'provider/forum',
        loadComponent: () => import('./pages/post-list.component/post-list.component').then((c) => c.PostListComponent)
      },
      {
        path: 'provider/forum/new',
        loadComponent: () => import('./pages/post-form.component/post-form.component').then((c) => c.PostFormComponent)
      },
      {
        path: 'provider/forum/edit/:id',
        loadComponent: () => import('./pages/post-form.component/post-form.component').then((c) => c.PostFormComponent)
      },
      {
        path: 'provider/forum/:id',
        loadComponent: () => import('./pages/post-detail.component/post-detail.component').then((c) => c.PostDetailComponent)
      },
      
     
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