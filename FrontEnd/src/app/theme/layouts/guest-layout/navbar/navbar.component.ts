import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  imports: [CommonModule, RouterModule, MatToolbarModule, MatButtonModule, MatIconModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent {
  menuOpen = false;

  constructor(private authService: AuthService) {}
  
  toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
  }

  loginWithKeycloak(): void {
    this.menuOpen = false;
    this.authService.loginWithKeycloak().catch((error) => {
      console.error('Keycloak login error:', error);
    });
  }
}
