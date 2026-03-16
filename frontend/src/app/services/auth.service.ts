import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';

export interface AuthResponse {
    token: string;
    username: string;
    email: string;
    message: string;
}

export interface UserInfo {
    username: string;
    email: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
    private readonly API_URL = 'http://localhost:8080/api/auth';
    private currentUserSubject = new BehaviorSubject<UserInfo | null>(null);

    currentUser$ = this.currentUserSubject.asObservable();

    constructor(private http: HttpClient) {
        this.loadStoredUser();
    }

    private loadStoredUser(): void {
        const token = localStorage.getItem('sv_token');
        const user = localStorage.getItem('sv_user');
        if (token && user) {
            this.currentUserSubject.next(JSON.parse(user));
        }
    }

    get isLoggedIn(): boolean {
        return !!localStorage.getItem('sv_token');
    }

    get token(): string | null {
        return localStorage.getItem('sv_token');
    }

    signup(username: string, email: string, password: string): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.API_URL}/signup`, { username, email, password })
            .pipe(tap(res => this.handleAuth(res)));
    }

    login(username: string, password: string): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.API_URL}/login`, { username, password })
            .pipe(tap(res => this.handleAuth(res)));
    }

    logout(): void {
        localStorage.removeItem('sv_token');
        localStorage.removeItem('sv_user');
        this.currentUserSubject.next(null);
    }

    private handleAuth(res: AuthResponse): void {
        localStorage.setItem('sv_token', res.token);
        const user: UserInfo = { username: res.username, email: res.email };
        localStorage.setItem('sv_user', JSON.stringify(user));
        this.currentUserSubject.next(user);
    }
}
