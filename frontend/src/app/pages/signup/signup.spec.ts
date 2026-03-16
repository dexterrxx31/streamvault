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
        component.onSubmit();
        expect(component.error).toBe('Please fill in all fields');
    });

    it('should show error if passwords do not match', () => {
        component.username = 'u';
        component.email = 'e';
        component.password = 'p1';
        component.confirmPassword = 'p2';
        component.onSubmit();
        expect(component.error).toBe('Passwords do not match');
    });

    it('should call auth service and navigate on success', () => {
        authServiceSpy.signup.mockReturnValue(of({}));
        component.username = 'new';
        component.email = 'new@e.com';
        component.password = 'pass123';
        component.confirmPassword = 'pass123';
        component.onSubmit();

        expect(authServiceSpy.signup).toHaveBeenCalledWith('new', 'new@e.com', 'pass123');
        expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    });

    it('should handle signup error', () => {
        authServiceSpy.signup.mockReturnValue(throwError(() => ({ error: { error: 'Exists' } })));
        component.username = 'old';
        component.email = 'old@e.com';
        component.password = 'p123456';
        component.confirmPassword = 'p123456';
        component.onSubmit();

        expect(component.loading).toBe(false);
        expect(component.error).toBe('Exists');
    });
});
