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
import { LandingComponent } from './landing';
import { provideRouter } from '@angular/router';

describe('LandingComponent', () => {
    let component: LandingComponent;
    let fixture: ComponentFixture<LandingComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [LandingComponent],
            providers: [provideRouter([])]
        }).compileComponents();

        fixture = TestBed.createComponent(LandingComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should render welcome message', () => {
        const compiled = fixture.nativeElement;
        expect(compiled.querySelector('h1').textContent).toContain('StreamVault');
    });
});
