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
import { SignupComponent } from './signup';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideRouter } from '@angular/router';

describe('SignupComponent', () => {
    let component: SignupComponent;
    let fixture: ComponentFixture<SignupComponent>;
    let authServiceSpy: any;
    let router: Router;

    beforeEach(async () => {
        authServiceSpy = {
            signup: vi.fn()
        };

        await TestBed.configureTestingModule({
            imports: [SignupComponent],
            providers: [
                { provide: AuthService, useValue: authServiceSpy },
                provideRouter([])
            ]
        }).compileComponents();

        router = TestBed.inject(Router);
        vi.spyOn(router, 'navigate');

        fixture = TestBed.createComponent(SignupComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should show error if form invalid', () => {
        component.username = '';
        component.onSignup();
        expect(component.error).toBe('Please fill in all fields');
    });

    it('should call auth service and navigate on success', () => {
        authServiceSpy.signup.mockReturnValue(of({}));
        component.username = 'new';
        component.email = 'new@e.com';
        component.password = 'pass';
        component.onSignup();

        expect(authServiceSpy.signup).toHaveBeenCalledWith('new', 'new@e.com', 'pass');
        expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    });

    it('should handle signup error', () => {
        authServiceSpy.signup.mockReturnValue(throwError(() => ({ error: { error: 'Exists' } })));
        component.username = 'old';
        component.email = 'old@e.com';
        component.password = 'p';
        component.onSignup();

        expect(component.loading).toBe(false);
        expect(component.error).toBe('Exists');
    });

    it('should use default error if server error empty', () => {
        authServiceSpy.signup.mockReturnValue(throwError(() => ({})));
        component.username = 'u';
        component.email = 'e';
        component.password = 'p';
        component.onSignup();
        expect(component.error).toBe('Signup failed. Please try again.');
    });
});
