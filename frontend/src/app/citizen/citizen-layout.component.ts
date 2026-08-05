import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { CITIZEN_NAV } from './citizen-nav';

@Component({
  selector: 'app-citizen-layout',
  standalone: false,
  templateUrl: './citizen-layout.component.html',
  styleUrls: ['./citizen-layout.component.css']
})
export class CitizenLayoutComponent {
  navGroups = CITIZEN_NAV;
  sidebarOpen = true;

  constructor(private authService: AuthService, private router: Router) {}

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
