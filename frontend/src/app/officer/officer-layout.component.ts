import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { OFFICER_NAV } from './officer-nav';

@Component({
  selector: 'app-officer-layout',
  standalone: false,
  templateUrl: './officer-layout.component.html',
  styleUrls: ['./officer-layout.component.css']
})
export class OfficerLayoutComponent {
  navGroups = OFFICER_NAV;
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
