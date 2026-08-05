import { Component, OnInit } from '@angular/core';

// Adjust these import paths to match your actual project layout.
import { UserService } from '../services/user.service';
import { UserDto } from '../models/user.model';

interface ProfileFormModel {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
}

interface PasswordFormModel {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

/**
 * Citizen's own profile. GET via UserService.getProfile() (/api/users/me,
 * ApiResponse-wrapped), PUT via the inherited base-CRUD update() (plain
 * DTO, not wrapped) using that same profile's id. Only exposes the
 * self-service-appropriate subset of UserUpdateRequest — role/badgeNumber/
 * isActive are left alone here, not sent in the request at all.
 */
@Component({
  selector: 'app-profile',
  standalone: false,
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {
  profile: UserDto | null = null;
  loading = false;
  loadError = '';

  form: ProfileFormModel = { firstName: '', lastName: '', email: '', phoneNumber: '' };
  savingProfile = false;
  profileError = '';
  profileSaved = false;

  passwordForm: PasswordFormModel = { currentPassword: '', newPassword: '', confirmPassword: '' };
  savingPassword = false;
  passwordError = '';
  passwordSaved = false;

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading = true;
    this.loadError = '';

    this.userService.getProfile().subscribe({
      next: (response) => {
        this.profile = response.data;
        this.form = {
          firstName: this.profile.firstName,
          lastName: this.profile.lastName,
          email: this.profile.email,
          phoneNumber: this.profile.phoneNumber
        };
        this.loading = false;
      },
      error: () => {
        this.loadError = 'Unable to load your profile.';
        this.loading = false;
      }
    });
  }

  saveProfile(): void {
    if (!this.profile) return;

    this.savingProfile = true;
    this.profileError = '';
    this.profileSaved = false;

    this.userService.update(this.profile.id, { ...this.form }).subscribe({
      next: (updated) => {
        this.profile = updated;
        this.savingProfile = false;
        this.profileSaved = true;
      },
      error: (err) => {
        this.savingProfile = false;
        this.profileError = err?.error?.message ?? 'Unable to save your profile.';
      }
    });
  }

  get passwordsMatch(): boolean {
    return (
      !!this.passwordForm.newPassword && this.passwordForm.newPassword === this.passwordForm.confirmPassword
    );
  }

  changePassword(): void {
    if (!this.profile || !this.passwordsMatch) return;

    this.savingPassword = true;
    this.passwordError = '';
    this.passwordSaved = false;

    this.userService
      .changePassword(this.profile.id, {
        currentPassword: this.passwordForm.currentPassword,
        newPassword: this.passwordForm.newPassword
      })
      .subscribe({
        next: () => {
          this.savingPassword = false;
          this.passwordSaved = true;
          this.passwordForm = { currentPassword: '', newPassword: '', confirmPassword: '' };
        },
        error: (err) => {
          this.savingPassword = false;
          this.passwordError = err?.error?.message ?? 'Unable to change your password.';
        }
      });
  }
}