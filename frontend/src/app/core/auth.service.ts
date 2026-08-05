import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { map } from 'rxjs/operators';

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  username: string;
  role: string;
}

const TOKEN_KEY = 'traffic-token';
const REFRESH_TOKEN_KEY = 'traffic-refresh-token';
const USER_KEY = 'traffic-user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private currentUserSubject = new BehaviorSubject<{ username: string; role: string } | null>(null);
  currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    const savedUser = localStorage.getItem(USER_KEY);
    if (savedUser) {
      this.currentUserSubject.next(JSON.parse(savedUser));
    }
  }

  login(username: string, password: string): Observable<any> {
    return this.http.post<any>('/api/auth/login', { username, password }).pipe(
      tap((response) => {
        const user = response?.data;
        if (user?.accessToken) {
          this.storeSession(user);
        }
      })
    );
  }

  register(payload: any): Observable<any> {
    return this.http.post<any>('/api/users', payload);
  }

  /**
   * Exchanges the stored refresh token for a new token pair
   * (AuthController.refresh, 13.2/13.5). Returns the new access token so
   * the interceptor can retry the request that triggered the refresh.
   *
   * Request body shape is an assumption — the doc confirms the route
   * exists and returns a LoginResponse, but not the exact request field
   * name. Verify `{ refreshToken }` against AuthController.java; adjust
   * here if it expects something else (e.g. a query param).
   */
  refresh(): Observable<string> {
    const refreshToken = this.getRefreshToken();
    return this.http.post<any>('/api/auth/refresh', { refreshToken }).pipe(
      map((response) => {
        const user = response?.data;
        if (!user?.accessToken) {
          throw new Error('Refresh response did not include an access token');
        }
        this.storeSession(user);
        return user.accessToken as string;
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUserSubject.next(null);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  getRole(): string | null {
    return this.currentUserSubject.value?.role ?? null;
  }

  private storeSession(user: LoginResponse): void {
    localStorage.setItem(TOKEN_KEY, user.accessToken);
    if (user.refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, user.refreshToken);
    }
    localStorage.setItem(USER_KEY, JSON.stringify({ username: user.username, role: user.role }));
    this.currentUserSubject.next({ username: user.username, role: user.role });
  }
}