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
import { PlayerComponent } from './player';
import { VideoService } from '../../services/video.service';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

describe('PlayerComponent', () => {
    let component: PlayerComponent;
    let fixture: ComponentFixture<PlayerComponent>;
    let videoServiceSpy: any;
    let routerSpy: any;
    let routeSpy: any;

    beforeEach(async () => {
        videoServiceSpy = {
            getVideoById: vi.fn(),
            getUserVideos: vi.fn().mockReturnValue(of([{
                id: 1,
                title: 'Test Video',
                contentType: 'video/mp4',
                size: 1024,
                uploadDate: '2024-03-15'
            }])),
            getStreamUrl: vi.fn().mockReturnValue('http://stream/1')
        };

        routerSpy = {
            navigate: vi.fn()
        };

        routeSpy = {
            params: of({ id: '1' })
        };

        await TestBed.configureTestingModule({
            imports: [PlayerComponent],
            providers: [
                { provide: VideoService, useValue: videoServiceSpy },
                { provide: Router, useValue: routerSpy },
                { provide: ActivatedRoute, useValue: routeSpy }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(PlayerComponent);
        component = fixture.componentInstance;
    });

    it('should create and load video info', () => {
        fixture.detectChanges();
        expect(component).toBeTruthy();
        expect(component.videoId).toBe(1);
        expect(component.video?.title).toBe('Test Video');
        expect(component.streamUrl).toBe('http://stream/1');
    });

    it('should navigate back to dashboard', () => {
        fixture.detectChanges();
        component.goBack();
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
    });
});
