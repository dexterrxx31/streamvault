import { describe, it, expect, beforeEach, vi } from 'vitest';
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

import { ComponentFixture } from '@angular/core/testing';
import { LoginComponent } from './login';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideRouter } from '@angular/router';

describe('LoginComponent', () => {
    let component: LoginComponent;
    let fixture: ComponentFixture<LoginComponent>;
    let authServiceSpy: any;
    let router: Router;

    beforeEach(async () => {
        authServiceSpy = {
            login: vi.fn()
        };

        await TestBed.configureTestingModule({
            imports: [LoginComponent],
            providers: [
                { provide: AuthService, useValue: authServiceSpy },
                provideRouter([])
            ]
        }).compileComponents();

        router = TestBed.inject(Router);
        vi.spyOn(router, 'navigate');

        fixture = TestBed.createComponent(LoginComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should show error if form invalid', () => {
        component.username = '';
        component.onSubmit();
        expect(component.error).toBe('Please fill in all fields');
    });

    it('should call auth service and navigate on success', () => {
        authServiceSpy.login.mockReturnValue(of({}));
        component.username = 'test';
        component.password = 'pass';
        component.onSubmit();

        expect(authServiceSpy.login).toHaveBeenCalledWith('test', 'pass');
        expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    });

    it('should handle login error', () => {
        authServiceSpy.login.mockReturnValue(throwError(() => ({ error: { error: 'Invalid' } })));
        component.username = 'test';
        component.password = 'pass';
        component.onSubmit();

        expect(component.loading).toBe(false);
        expect(component.error).toBe('Invalid');
    });

    it('should use generic error message if server error empty', () => {
        authServiceSpy.login.mockReturnValue(throwError(() => ({})));
        component.username = 'test';
        component.password = 'pass';
        component.onSubmit();
        expect(component.error).toBe('Login failed. Please try again.');
    });
});
