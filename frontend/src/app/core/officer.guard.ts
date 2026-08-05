import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const officerGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.getToken()) {
    router.navigate(['/login']);
    return false;
  }

  const role = authService.getRole();
  if (role !== 'OFFICER' && role !== 'ADMIN') {
    router.navigate(['/dashboard']);
    return false;
  }

  return true;
};
