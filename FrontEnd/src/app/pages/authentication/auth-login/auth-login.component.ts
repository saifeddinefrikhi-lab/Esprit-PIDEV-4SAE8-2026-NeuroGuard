import { Component, AfterViewInit, NgZone, ChangeDetectorRef } from '@angular/core';
declare var google: any;
import { AuthService } from '../../../core/services/auth.service';  
import { Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-auth-login',
  templateUrl: './auth-login.component.html',
  styleUrls: ['./auth-login.component.scss'],
  imports: [ReactiveFormsModule, RouterModule, CommonModule]
})
export class AuthLoginComponent implements AfterViewInit {
  loginForm: FormGroup;
  submitted = false;
  errorMessage = '';
  successMessage = '';
  isLoading = false;
  showPassword = false;
  
  // Google Role Selection Modal
  public showRoleModal = false;
  public pendingGoogleToken = '';

  constructor(
    private authService: AuthService, 
    private router: Router,
    private fb: FormBuilder,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  ngAfterViewInit(): void {
    this.initializeGoogleSignIn();
  }

  private initializeGoogleSignIn(): void {
    if (typeof google !== 'undefined') {
      google.accounts.id.initialize({
        client_id: '550789921754-tdpg2nso52gvhr2mgdhk0ra01hk79kt8.apps.googleusercontent.com', // Replace with your actual Client ID
        callback: (response: any) => this.ngZone.run(() => this.handleGoogleCredential(response))
      });

      google.accounts.id.renderButton(
        document.getElementById('google-btn-container'),
        { theme: 'outline', size: 'large', shape: 'rectangular' } // Removed explicit 100% to fix warning
      );
    } else {
      setTimeout(() => this.initializeGoogleSignIn(), 500);
    }
  }

  private handleGoogleCredential(response: any): void {
    console.log('Received Google credential');
    const credential = response.credential;
    this.isLoading = true;
    
    this.authService.googleLogin(credential).subscribe({
      next: (res) => {
        if (res && res.newUser) {
          console.log('New user detected, showing role selection');
          this.pendingGoogleToken = credential;
          this.showRoleModal = true;
          this.isLoading = false;
          this.cdr.detectChanges(); // Manual change detection
          return;
        }

        this.successMessage = 'Google login successful! Redirecting...';
        this.isLoading = false;
        setTimeout(() => {
          const token = localStorage.getItem('authToken');
          if (token) {
            this.authService.redirectBasedOnRole(this.authService.getRoleFromToken(token));
          }
        }, 1500);
      },
      error: (error) => {
        console.error('Google login error:', error);
        this.errorMessage = error.message || 'Google authentication failed.';
        this.isLoading = false;
      }
    });
  }

  selectRole(role: string): void {
    if (!this.pendingGoogleToken) return;
    
    this.isLoading = true;
    this.showRoleModal = false;
    
    this.authService.googleComplete(this.pendingGoogleToken, role).subscribe({
      next: () => {
        this.successMessage = 'Account created successfully! Redirecting...';
        this.isLoading = false;
        this.pendingGoogleToken = '';
        
        setTimeout(() => {
          const token = localStorage.getItem('authToken');
          if (token) {
            this.authService.redirectBasedOnRole(this.authService.getRoleFromToken(token));
          }
        }, 1500);
      },
      error: (err) => {
        console.error('Registration completion failed:', err);
        this.errorMessage = err.message || 'Failed to complete registration.';
        this.isLoading = false;
        this.pendingGoogleToken = '';
      }
    });
  }

  get f() {
    return this.loginForm.controls;
  }

  getUsernameErrorMessage(): string {
    const control = this.f['username'];
    if (control.hasError('required')) {
      return 'Username is required';
    }
    if (control.hasError('minlength')) {
      return 'Username must be at least 3 characters';
    }
    return '';
  }

  getPasswordErrorMessage(): string {
    const control = this.f['password'];
    if (control.hasError('required')) {
      return 'Password is required';
    }
    if (control.hasError('minlength')) {
      return 'Password must be at least 6 characters';
    }
    return '';
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  onLogin(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (this.loginForm.invalid) {
      console.log('Form is invalid');
      return;
    }

    this.isLoading = true;
    this.loginForm.disable();
    
    const credentials = this.loginForm.value;
    console.log('Attempting login...');

    this.authService.login(credentials).subscribe({
      next: (response) => {
        console.log('Login successful');
        this.isLoading = false;
        this.loginForm.enable();
        this.successMessage = 'Login successful! Redirecting...';
        
        const token = localStorage.getItem('authToken');
        if (token) {
          const userRole = this.authService.getRoleFromToken(token);
          console.log('User role:', userRole);
          setTimeout(() => {
            this.authService.redirectBasedOnRole(userRole);
          }, 1500);
        }
      },
      error: (error) => {
        console.error('Login error:', error);
        this.isLoading = false;
        this.loginForm.enable();
        
        if (error.message) {
          this.errorMessage = error.message;
        } else if (error.error) {
          this.errorMessage = typeof error.error === 'string' ? error.error : 'Invalid credentials';
        } else {
          this.errorMessage = 'Login failed. Please check your credentials.';
        }
        
        this.loginForm.markAllAsTouched();
      }
    });
  }
}