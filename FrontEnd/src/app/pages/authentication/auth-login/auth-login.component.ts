import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';  
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-auth-login',
  templateUrl: './auth-login.component.html',
  styleUrls: ['./auth-login.component.scss'],
  imports: [RouterModule, CommonModule]
})
export class AuthLoginComponent implements OnInit {
  errorMessage = '';
  successMessage = '';
  isLoading = false;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    // If already authenticated (e.g. user navigated to /login manually), redirect immediately
    if (this.authService.isKeycloakAuthenticated()) {
      this.authService.syncKeycloakUser();
      if (this.authService.currentUser) {
        this.authService.redirectBasedOnRole(this.authService.currentUser.role);
        return;
      }
    }

    // Not authenticated yet — trigger Keycloak login, returns to /auth-callback
    this.loginWithKeycloak();
  }

  loginWithKeycloak(): void {
    this.errorMessage = '';
    this.isLoading = true;
    this.authService.loginWithKeycloak().catch((error) => {
      console.error('Keycloak login error:', error);
      this.errorMessage = 'Connexion Keycloak impossible. Vérifiez que Keycloak est démarré.';
      this.isLoading = false;
    });
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}