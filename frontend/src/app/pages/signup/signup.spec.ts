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
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
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
            imports: [SignupComponent, FormsModule],
            providers: [
                { provide: AuthService, useValue: authServiceSpy },
                provideRouter([])
            ]
        }).compileComponents();

        router = TestBed.inject(Router);
        vi.spyOn(router, 'navigate');

        fixture = TestBed.createComponent(SignupComponent);
        component = fixture.componentInstance;
        // Do not call detectChanges here to avoid ExpressionChangedAfterItHasBeenCheckedError
    });

    it('should create', () => {
        fixture.detectChanges();
        expect(component).toBeTruthy();
    });

    it('should show error if passwords do not match', () => {
        component.username = 'test';
        component.email = 'e@e.com';
        component.password = 'password';
        component.confirmPassword = 'mismatch';
        component.onSubmit();
        expect(component.error).toBe('Passwords do not match');
    });

    it('should show error if password too short', () => {
        component.username = 'test';
        component.email = 'e@e.com';
        component.password = '123';
        component.confirmPassword = '123';
        component.onSubmit();
        expect(component.error).toBe('Password must be at least 6 characters');
    });

    it('should call signup and navigate on success', () => {
        authServiceSpy.signup.mockReturnValue(of({}));
        component.username = 'test';
        component.email = 'e@e.com';
        component.password = 'password';
        component.confirmPassword = 'password';

        component.onSubmit();

        expect(authServiceSpy.signup).toHaveBeenCalledWith('test', 'e@e.com', 'password');
        expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    });

    it('should handle signup error', () => {
        authServiceSpy.signup.mockReturnValue(throwError(() => ({ error: { error: 'Email taken' } })));
        component.username = 'test';
        component.email = 'taken@e.com';
        component.password = 'password';
        component.confirmPassword = 'password';

        component.onSubmit();

        expect(component.loading).toBe(false);
        expect(component.error).toBe('Email taken');
    });
});
