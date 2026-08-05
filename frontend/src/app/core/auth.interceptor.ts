import { Injectable } from '@angular/core';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest
} from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, filter, switchMap, take } from 'rxjs/operators';
import { AuthService } from './auth.service';

// Requests that should never trigger a refresh-and-retry cycle: retrying
// a failed login makes no sense, and retrying the refresh call itself
// would loop forever if the refresh token is also invalid.
const AUTH_EXEMPT_PATHS = ['/api/auth/login', '/api/auth/refresh'];

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  // Guards against a refresh storm: if five requests 401 at once, only the
  // first should call /api/auth/refresh — the rest wait on this subject
  // and retry with whatever token that first call comes back with.
  private isRefreshing = false;
  private refreshedToken$ = new BehaviorSubject<string | null>(null);

  constructor(private authService: AuthService, private router: Router) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.authService.getToken();
    const exempt = this.isExempt(req.url);

    const authReq = token && !exempt ? this.withToken(req, token) : req;

    return next.handle(authReq).pipe(
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 401 && !exempt) {
          return this.handle401(req, next);
        }
        return throwError(() => error);
      })
    );
  }

  private handle401(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!this.authService.getRefreshToken()) {
      this.forceLogout();
      return throwError(() => new Error('Session expired'));
    }

    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshedToken$.next(null);

      return this.authService.refresh().pipe(
        switchMap((newToken) => {
          this.isRefreshing = false;
          this.refreshedToken$.next(newToken);
          return next.handle(this.withToken(req, newToken));
        }),
        catchError((refreshError) => {
          this.isRefreshing = false;
          this.forceLogout();
          return throwError(() => refreshError);
        })
      );
    }

    // A refresh is already in flight for another request — wait for it
    // to land instead of firing a second /api/auth/refresh call.
    return this.refreshedToken$.pipe(
      filter((token): token is string => token !== null),
      take(1),
      switchMap((token) => next.handle(this.withToken(req, token)))
    );
  }

  private withToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
    return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  private isExempt(url: string): boolean {
    return AUTH_EXEMPT_PATHS.some((path) => url.includes(path));
  }

  private forceLogout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}