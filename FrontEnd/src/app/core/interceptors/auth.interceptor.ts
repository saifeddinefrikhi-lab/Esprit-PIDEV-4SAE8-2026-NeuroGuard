import { Injectable, inject } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable, from, switchMap } from 'rxjs';
import Keycloak from 'keycloak-js';

/**
 * AuthInterceptor
 * Attaches the Keycloak Bearer token to every API request.
 * Uses keycloak-js updateToken() directly to handle token refresh.
 */
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private readonly keycloak = inject(Keycloak, { optional: true });

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Skip Keycloak's own endpoints
    const isKeycloakRequest =
      req.url.includes('/realms/') ||
      req.url.includes('/protocol/openid-connect/');

    if (isKeycloakRequest) {
      return next.handle(req);
    }

    // If Keycloak is authenticated, refresh token if needed then attach it
    if (this.keycloak?.authenticated) {
      return from(
        // Refresh if token expires in less than 30 seconds
        this.keycloak.updateToken(30).catch(() => {
          // Token refresh failed — force re-login
          this.keycloak!.login({ redirectUri: `${window.location.origin}/auth-callback` });
          return false;
        })
      ).pipe(
        switchMap(() => {
          const token = this.keycloak?.token;
          if (token) {
            const authReq = req.clone({
              setHeaders: { Authorization: `Bearer ${token}` }
            });
            return next.handle(authReq);
          }
          return next.handle(req);
        })
      );
    }

    // No Keycloak session — pass through
    return next.handle(req);
  }
}
