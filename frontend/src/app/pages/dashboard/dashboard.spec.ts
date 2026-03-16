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

import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DashboardComponent } from './dashboard';
import { VideoService } from '../../services/video.service';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

describe('DashboardComponent', () => {
    let component: DashboardComponent;
    let fixture: ComponentFixture<DashboardComponent>;
    let videoServiceSpy: any;
    let router: Router;

    const mockVideos = [
        { id: 1, title: 'V1', contentType: 'v/mp4', size: 100, uploadDate: '' },
        { id: 2, title: 'V2', contentType: 'v/mp4', size: 200, uploadDate: '' }
    ];

    beforeEach(async () => {
        videoServiceSpy = {
            getUserVideos: vi.fn().mockReturnValue(of(mockVideos)),
            deleteVideo: vi.fn().mockReturnValue(of({}))
        };

        await TestBed.configureTestingModule({
            imports: [DashboardComponent],
            providers: [
                { provide: VideoService, useValue: videoServiceSpy },
                provideRouter([])
            ]
        }).compileComponents();

        router = TestBed.inject(Router);
        vi.spyOn(router, 'navigate');

        fixture = TestBed.createComponent(DashboardComponent);
        component = fixture.componentInstance;
    });

    it('should create and load videos', () => {
        fixture.detectChanges();
        expect(component.videos.length).toBe(2);
        expect(videoServiceSpy.getUserVideos).toHaveBeenCalled();
        expect(component.loading).toBe(false);
    });

    it('should toggle upload dialog', () => {
        fixture.detectChanges();
        component.openUpload();
        expect(component.showUploadDialog).toBe(true);
        component.closeUpload();
        expect(component.showUploadDialog).toBe(false);
    });

    it('should navigate to player', () => {
        fixture.detectChanges();
        component.playVideo(5);
        expect(router.navigate).toHaveBeenCalledWith(['/play', 5]);
    });

    it('should delete video and update list', () => {
        fixture.detectChanges();
        component.deleteVideo(1);
        expect(videoServiceSpy.deleteVideo).toHaveBeenCalledWith(1);
        expect(component.videos.length).toBe(1);
        expect(component.videos[0].id).toBe(2);
    });

    it('should refresh list onUploaded', () => {
        fixture.detectChanges();
        videoServiceSpy.getUserVideos.mockClear();
        component.onUploaded();
        expect(component.showUploadDialog).toBe(false);
        expect(videoServiceSpy.getUserVideos).toHaveBeenCalled();
    });
});
