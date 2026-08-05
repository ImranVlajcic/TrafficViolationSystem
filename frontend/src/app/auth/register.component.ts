import { Component } from '@angular/core';
import { AuthService } from '../core/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  standalone: false,
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  // Public self-registration always creates a CITIZEN account.
  // Officer/admin accounts should be provisioned by an admin, not chosen here.
  readonly role = 'CITIZEN';

  form = {
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phoneNumber: ''
  };
  errorMessage = '';
  successMessage = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router) {}

  register(): void {
    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.register({ ...this.form, role: this.role }).subscribe({
      next: () => {
        this.successMessage = 'Account created successfully. You can now sign in.';
        this.loading = false;
        setTimeout(() => this.router.navigate(['/login']), 1000);
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Registration failed.';
        this.loading = false;
      }
    });
  }
}
