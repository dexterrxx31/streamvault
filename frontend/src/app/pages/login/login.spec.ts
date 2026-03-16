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
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { of, throwError } from 'rxjs';
import { By } from '@angular/platform-browser';
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
            imports: [LoginComponent, FormsModule],
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

    it('should show error if credentials missing', () => {
        component.username = '';
        component.password = '';
        component.onSubmit();
        expect(component.error).toBe('Please fill in all fields');
    });

    it('should call login on service and navigate on success', () => {
        authServiceSpy.login.mockReturnValue(of({}));
        component.username = 'test';
        component.password = 'pass';

        component.onSubmit();

        expect(authServiceSpy.login).toHaveBeenCalledWith('test', 'pass');
        expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    });

    it('should handle login error', () => {
        authServiceSpy.login.mockReturnValue(throwError(() => ({ error: { error: 'Invalid creds' } })));
        component.username = 'test';
        component.password = 'wrong';

        component.onSubmit();

        expect(component.loading).toBe(false);
        expect(component.error).toBe('Invalid creds');
    });

    it('should display error message in template', () => {
        component.error = 'My Test Error';
        fixture.detectChanges();
        const errorEl = fixture.debugElement.query(By.css('.auth-error'));
        expect(errorEl.nativeElement.textContent).toContain('My Test Error');
    });
});
