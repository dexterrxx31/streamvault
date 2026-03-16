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

    const mockVideos = [
        { id: 123, title: 'Test Video', contentType: 'v/mp4', size: 100, uploadDate: '2024-03-15' }
    ];

    beforeEach(async () => {
        videoServiceSpy = {
            getStreamUrl: vi.fn().mockReturnValue('http://stream/123'),
            getUserVideos: vi.fn().mockReturnValue(of(mockVideos))
        };
        routerSpy = {
            navigate: vi.fn()
        };

        await TestBed.configureTestingModule({
            imports: [PlayerComponent],
            providers: [
                { provide: VideoService, useValue: videoServiceSpy },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        params: of({ id: '123' })
                    }
                },
                { provide: Router, useValue: routerSpy }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(PlayerComponent);
        component = fixture.componentInstance;
    });

    it('should create and load video info', () => {
        fixture.detectChanges();
        expect(component.videoId).toBe(123);
        expect(component.streamUrl).toBe('http://stream/123');
        expect(component.video?.title).toBe('Test Video');
        expect(component.loading).toBe(false);
    });

    it('should navigate back to dashboard', () => {
        fixture.detectChanges();
        component.goBack();
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
    });
});
