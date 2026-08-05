import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { UserService } from '../services/user.service';
import { UserDto, UserSearchObject } from '../models/user.model';
import { UserRole } from '../models/enums';
import { DataTableColumn } from '../shared/data-table/data-table.component';

const ROLE_OPTIONS: { value: UserRole; label: string }[] = [
  { value: 'ADMIN', label: 'Admin' },
  { value: 'OFFICER', label: 'Officer' },
  { value: 'CITIZEN', label: 'Citizen' },
  { value: 'SYSTEM', label: 'System' }
];

const PAGE_SIZE = 20;

/**
 * Admin user directory — GET /api/users (search), DELETE /api/users/{id}
 * (soft-delete, gated by app-confirm-dialog same as the destructive
 * actions on the officer side). Role/isActive filters map straight onto
 * UserSearchObject; the role select here is legitimate — unlike the
 * public register form, only an admin reaches this list/form.
 */
@Component({
  selector: 'app-user-list',
  standalone: false,
  templateUrl: './user-list.component.html'
})
export class UserListComponent implements OnInit {
  rows: UserDto[] = [];
  loading = false;
  error = '';

  page = 1;
  hasMore = false;
  count = 0;

  search = '';
  roleFilter: UserRole | '' = '';
  activeFilter: '' | 'true' | 'false' = '';

  roleOptions = ROLE_OPTIONS;

  deleteTarget: UserDto | null = null;
  deleting = false;
  deleteError = '';

  columns: DataTableColumn<UserDto>[] = [
    { key: 'username', label: 'Username', mono: true },
    { key: 'email', label: 'Email' },
    { key: 'firstName', label: 'Name', format: (row) => `${row.firstName} ${row.lastName}` },
    { key: 'role', label: 'Role', align: 'center' },
    { key: 'badgeNumber', label: 'Badge #', mono: true },
    { key: 'isActive', label: 'Status', align: 'center', format: (row) => (row.isActive ? 'Active' : 'Inactive') },
    {
      key: 'lastLogInAt',
      label: 'Last login',
      format: (row) => (row.lastLogInAt ? new Date(row.lastLogInAt).toLocaleString() : 'Never')
    }
  ];

  constructor(private userService: UserService, private router: Router) {}

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading = true;
    this.error = '';

    const searchObject: UserSearchObject = {
      page: this.page,
      limit: PAGE_SIZE,
      includeCount: true,
      search: this.search.trim() || undefined,
      role: this.roleFilter || undefined,
      isActive: this.activeFilter === '' ? undefined : this.activeFilter === 'true'
    };

    this.userService.search(searchObject).subscribe({
      next: (result) => {
        this.rows = result?.resultList ?? [];
        this.count = result?.count ?? 0;
        this.hasMore = result?.hasMore ?? false;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load users.';
        this.loading = false;
      }
    });
  }

  onFilterSubmit(): void {
    this.page = 1;
    this.load();
  }

  onPageChange(page: number): void {
    this.page = page;
    this.load();
  }

  createNew(): void {
    this.router.navigate(['/admin/users/new']);
  }

  edit(user: UserDto): void {
    this.router.navigate(['/admin/users', user.id, 'edit']);
  }

  confirmDelete(user: UserDto): void {
    this.deleteError = '';
    this.deleteTarget = user;
  }

  cancelDelete(): void {
    this.deleteTarget = null;
  }

  get deleteMessage(): string {
    return this.deleteTarget
      ? `Deactivate ${this.deleteTarget.username}? This can be reversed later by another admin.`
      : '';
  }

  doDelete(): void {
    if (!this.deleteTarget) return;

    this.deleting = true;
    this.deleteError = '';

    this.userService.deleteUser(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleting = false;
        this.deleteTarget = null;
        this.load();
      },
      error: () => {
        this.deleting = false;
        this.deleteError = 'Unable to delete this user.';
      }
    });
  }
}
