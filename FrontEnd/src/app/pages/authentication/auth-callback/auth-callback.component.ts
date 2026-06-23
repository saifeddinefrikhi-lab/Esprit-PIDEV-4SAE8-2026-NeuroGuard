import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import Keycloak from 'keycloak-js';
import { AuthService } from '../../../core/services/auth.service';

/**
 * Auth Callback Component
 * Landing page after Keycloak redirects back to the app.
 * Reads the Keycloak token, extracts role and userId, then redirects to the correct dashboard.
 */
@Component({
  selector: 'app-auth-callback',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="min-height:100vh;display:flex;align-items:center;justify-content:center;flex-direction:column;gap:16px;">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
      <p class="text-muted">Authenticating, please wait...</p>
      <p class="text-danger small" *ngIf="errorMessage">{{ errorMessage }}</p>
    </div>
  `
})
export class AuthCallbackComponent implements OnInit {
  private readonly keycloak = inject(Keycloak, { optional: true });
  errorMessage = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.handleCallback();
  }

  private handleCallback(): void {
    if (!this.keycloak) {
      this.errorMessage = 'Keycloak not available.';
      setTimeout(() => this.router.navigate(['/login']), 2000);
      return;
    }

    if (this.keycloak.authenticated) {
      // Token is available — sync user and redirect
      this.authService.syncKeycloakUser();

      const user = this.authService.currentUser;
      if (user && user.role) {
        console.log('[AuthCallback] Authenticated as:', user.role, '| userId:', user.userId);
        this.authService.redirectBasedOnRole(user.role);
      } else {
        // Unlikely but handle gracefully
        this.errorMessage = 'Could not determine your role. Please contact support.';
        console.error('[AuthCallback] syncKeycloakUser succeeded but no currentUser. tokenParsed:', this.keycloak.tokenParsed);
        setTimeout(() => this.router.navigate(['/restricted']), 3000);
      }
    } else {
      // Not authenticated — send back to login
      console.warn('[AuthCallback] Keycloak not authenticated on callback. Redirecting to login.');
      this.router.navigate(['/login']);
    }
  }
}
