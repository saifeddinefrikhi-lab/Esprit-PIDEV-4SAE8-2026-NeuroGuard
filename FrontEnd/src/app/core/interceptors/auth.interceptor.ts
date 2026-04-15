import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service'; // adjust path

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private auth: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const isAuthRequest = req.url.includes('/auth/login') || req.url.includes('/auth/register');
    if (isAuthRequest) {
      return next.handle(req);
    }

    const token = this.auth.getToken(); // reads from localStorage
    if (!token) {
      return next.handle(req);
    }

    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });

    return next.handle(req);
  }
}