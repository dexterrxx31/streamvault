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
import { NavbarComponent } from './navbar';
import { AuthService, UserInfo } from '../../services/auth.service';
import { Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { provideRouter } from '@angular/router';

describe('NavbarComponent', () => {
    let component: NavbarComponent;
    let fixture: ComponentFixture<NavbarComponent>;
    let authServiceSpy: any;
    let router: Router;
    let currentUserSubject: BehaviorSubject<UserInfo | null>;

    beforeEach(async () => {
        currentUserSubject = new BehaviorSubject<UserInfo | null>(null);
        authServiceSpy = {
            currentUser$: currentUserSubject.asObservable(),
            logout: vi.fn()
        };

        await TestBed.configureTestingModule({
            imports: [NavbarComponent],
            providers: [
                { provide: AuthService, useValue: authServiceSpy },
                provideRouter([])
            ]
        }).compileComponents();

        router = TestBed.inject(Router);
        vi.spyOn(router, 'navigate');

        fixture = TestBed.createComponent(NavbarComponent);
        component = fixture.componentInstance;
    });

    it('should create', () => {
        fixture.detectChanges();
        expect(component).toBeTruthy();
    });

    it('should display login/signup when not logged in', () => {
        currentUserSubject.next(null);
        fixture.detectChanges();
        const compiled = fixture.nativeElement;
        expect(compiled.querySelector('a[routerLink="/login"]')).toBeTruthy();
        expect(compiled.querySelector('a[routerLink="/signup"]')).toBeTruthy();
    });

    it('should display user info and logout when logged in', () => {
        const user: UserInfo = { username: 'testuser', email: 't@t.com' };
        currentUserSubject.next(user);
        fixture.detectChanges();

        const compiled = fixture.nativeElement;
        expect(compiled.textContent).toContain('testuser');
        expect(compiled.querySelector('.btn-ghost')).toBeTruthy(); // Logout button
    });

    it('should call logout and navigate on logout click', () => {
        currentUserSubject.next({ username: 'test', email: 'e' });
        fixture.detectChanges();

        component.logout();
        expect(authServiceSpy.logout).toHaveBeenCalled();
        expect(router.navigate).toHaveBeenCalledWith(['/']);
    });
});
