import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { UserService } from '../services/user.service';
import { UserDto, UserCreateRequest, UserUpdateRequest } from '../models/user.model';
import { UserRole } from '../models/enums';

const ROLE_OPTIONS: { value: UserRole; label: string }[] = [
  { value: 'ADMIN', label: 'Admin' },
  { value: 'OFFICER', label: 'Officer' },
  { value: 'CITIZEN', label: 'Citizen' },
  { value: 'SYSTEM', label: 'System' }
];

/**
 * Admin create/edit user, reached via /admin/users/new and
 * /admin/users/:id/edit. The role select is legitimate here — unlike the
 * public register form, only an admin reaches this one.
 *
 * Edit mode reuses UserUpdateRequest.password (optional) to let an admin
 * reset a password directly. That's distinct from ChangePasswordRequest,
 * which requires the *current* password and is for self-service only
 * (UserService.changePassword) — not used on this screen.
 *
 * username has no field on UserUpdateRequest, so it's shown read-only
 * in edit mode rather than sent back to the server.
 */
@Component({
  selector: 'app-user-form',
  standalone: false,
  templateUrl: './user-form.component.html',
  styleUrls: ['./user-form.component.css']
})
export class UserFormComponent implements OnInit {
  mode: 'create' | 'edit' = 'create';
  userId = '';

  loading = false;
  loadError = '';
  submitting = false;
  submitError = '';

  roleOptions = ROLE_OPTIONS;

  username = '';
  email = '';
  password = '';
  firstName = '';
  lastName = '';
  phoneNumber = '';
  role: UserRole = 'CITIZEN';
  badgeNumber = '';
  isActive = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.mode = 'edit';
      this.userId = id;
      this.loadUser(id);
    }
  }

  private loadUser(id: string): void {
    this.loading = true;
    this.loadError = '';

    this.userService.findById(id).subscribe({
      next: (user: UserDto) => {
        this.username = user.username;
        this.email = user.email;
        this.firstName = user.firstName;
        this.lastName = user.lastName;
        this.phoneNumber = user.phoneNumber;
        this.role = user.role;
        this.badgeNumber = user.badgeNumber ?? '';
        this.isActive = user.isActive;
        this.loading = false;
      },
      error: () => {
        this.loadError = 'Unable to load this user.';
        this.loading = false;
      }
    });
  }

  get canSubmit(): boolean {
    if (this.submitting || this.loading) return false;
    if (this.mode === 'create' && (!this.username || !this.password)) return false;
    return !!this.email && !!this.firstName && !!this.lastName && !!this.phoneNumber;
  }

  submit(): void {
    if (this.mode === 'create') {
      this.submitCreate();
    } else {
      this.submitUpdate();
    }
  }

  private submitCreate(): void {
    const request: UserCreateRequest = {
      username: this.username,
      email: this.email,
      password: this.password,
      firstName: this.firstName,
      lastName: this.lastName,
      phoneNumber: this.phoneNumber,
      role: this.role,
      badgeNumber: this.badgeNumber || undefined
    };

    this.submitting = true;
    this.submitError = '';

    this.userService.create(request).subscribe({
      next: () => {
        this.submitting = false;
        this.router.navigate(['/admin/users']);
      },
      error: (err) => {
        this.submitting = false;
        this.submitError = err?.error?.message ?? 'Unable to create this user.';
      }
    });
  }

  private submitUpdate(): void {
    const request: UserUpdateRequest = {
      email: this.email,
      firstName: this.firstName,
      lastName: this.lastName,
      phoneNumber: this.phoneNumber,
      role: this.role,
      badgeNumber: this.badgeNumber || undefined,
      isActive: this.isActive,
      password: this.password || undefined
    };

    this.submitting = true;
    this.submitError = '';

    this.userService.update(this.userId, request).subscribe({
      next: () => {
        this.submitting = false;
        this.router.navigate(['/admin/users']);
      },
      error: (err) => {
        this.submitting = false;
        this.submitError = err?.error?.message ?? 'Unable to update this user.';
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/users']);
  }
}
