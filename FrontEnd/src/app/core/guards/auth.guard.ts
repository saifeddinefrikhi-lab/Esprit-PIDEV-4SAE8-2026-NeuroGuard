import { inject } from '@angular/core';
import { Router, CanActivateFn, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';
import Keycloak from 'keycloak-js';

export const authGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const keycloak = inject(Keycloak, { optional: true });

  const isKeycloakLoggedIn = keycloak?.authenticated ?? false;
  const isLocalLoggedIn = authService.isLoggedIn;

  // Not authenticated at all → go to login
  if (!isKeycloakLoggedIn && !isLocalLoggedIn) {
    if (keycloak) {
      keycloak.login({ redirectUri: `${window.location.origin}/auth-callback` });
    } else {
      router.navigate(['/login']);
    }
    return false;
  }

  // Sync Keycloak user on every guard check to ensure role is always fresh
  if (isKeycloakLoggedIn) {
    authService.syncKeycloakUser();
  }

  const requiredRoles = route.data['roles'] as string[] | undefined;

  // Route has no role restriction (e.g. /homePage) → redirect to user's own dashboard
  if (!requiredRoles || requiredRoles.length === 0) {
    if (authService.currentUser) {
      authService.redirectBasedOnRole(authService.currentUser.role);
      return false; // prevent loading homePage; redirect handles navigation
    }
    return true;
  }

  // Route has role restriction → check user's role
  if (authService.currentUser) {
    const userRole = authService.currentUser.role.toUpperCase();
    const matched = requiredRoles.some(r => r.toUpperCase() === userRole);
    if (!matched) {
      console.warn(`[AuthGuard] Access denied. User role: ${userRole}, required: ${requiredRoles}`);
      // Redirect to the user's own dashboard instead of /restricted
      authService.redirectBasedOnRole(userRole);
      return false;
    }
    return true;
  }

  // Fallback via Keycloak hasRealmRole
  if (isKeycloakLoggedIn && keycloak) {
    const hasRole = requiredRoles.some((role) =>
      keycloak.hasRealmRole(role) ||
      keycloak.hasRealmRole(role.toLowerCase()) ||
      keycloak.hasRealmRole(role.toUpperCase())
    );
    if (!hasRole) {
      router.navigate(['/restricted']);
      return false;
    }
    return true;
  }

  return true;
};