import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import 'zone.js';
import 'zone.js/testing';
import { TestBed, getTestBed } from '@angular/core/testing';
import {
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';

try {
    getTestBed().initTestEnvironment(
        BrowserDynamicTestingModule,
        platformBrowserDynamicTesting(),
        { teardown: { destroyAfterEach: false } }
    );
} catch (e) { }

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService, AuthResponse, UserInfo } from './auth.service';

describe('AuthService', () => {
    let service: AuthService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [AuthService]
        });
        service = TestBed.inject(AuthService);
        httpMock = TestBed.inject(HttpTestingController);
        localStorage.clear();
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('login', () => {
        it('should perform login and store user data', () => {
            const mockResponse: AuthResponse = {
                token: 'mock-token',
                username: 'testuser',
                email: 'test@example.com',
                message: 'Login successful'
            };

            service.login('testuser', 'password').subscribe(res => {
                expect(res).toEqual(mockResponse);
                expect(localStorage.getItem('sv_token')).toBe('mock-token');
                const storedUser = JSON.parse(localStorage.getItem('sv_user') || '{}');
                expect(storedUser.username).toBe('testuser');
            });

            const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
            expect(req.request.method).toBe('POST');
            req.flush(mockResponse);
        });
    });

    describe('signup', () => {
        it('should perform signup and store user data', () => {
            const mockResponse: AuthResponse = {
                token: 'new-token',
                username: 'newuser',
                email: 'new@example.com',
                message: 'Signup successful'
            };

            service.signup('newuser', 'new@example.com', 'password').subscribe(res => {
                expect(res).toEqual(mockResponse);
                expect(localStorage.getItem('sv_token')).toBe('new-token');
            });

            const req = httpMock.expectOne('http://localhost:8080/api/auth/signup');
            expect(req.request.method).toBe('POST');
            req.flush(mockResponse);
        });
    });

    describe('logout', () => {
        it('should clear local storage and current user', async () => {
            localStorage.setItem('sv_token', 'token');
            localStorage.setItem('sv_user', JSON.stringify({ username: 'user', email: 'e' }));

            service.logout();

            expect(localStorage.getItem('sv_token')).toBeNull();
            expect(localStorage.getItem('sv_user')).toBeNull();

            const user = await new Promise(resolve => {
                service.currentUser$.subscribe(u => resolve(u));
            });
            expect(user).toBeNull();
        });
    });

    describe('getters', () => {
        it('should return correct isLoggedIn value', () => {
            expect(service.isLoggedIn).toBe(false);
            localStorage.setItem('sv_token', 'some-token');
            expect(service.isLoggedIn).toBe(true);
        });

        it('should return correct token value', () => {
            expect(service.token).toBeNull();
            localStorage.setItem('sv_token', 'abc');
            expect(service.token).toBe('abc');
        });
    });
});
