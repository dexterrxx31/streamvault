import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
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

    const makeVideo = (id: number, title: string, status: string = 'READY') => ({
        id, title, contentType: 'v/mp4', size: 100, uploadDate: '',
        durationSeconds: 60, width: null, height: null, description: null,
        tags: [], status, hasThumbnail: false
    });

    const mockVideos = [makeVideo(1, 'V1'), makeVideo(2, 'V2')];

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

    describe('processing poll', () => {
        beforeEach(() => {
            vi.useFakeTimers();
        });

        afterEach(() => {
            vi.useRealTimers();
        });

        it('should poll while a video is PROCESSING and stop when READY', () => {
            videoServiceSpy.getUserVideos.mockReturnValue(
                of([makeVideo(1, 'V1', 'PROCESSING')])
            );
            fixture.detectChanges();
            videoServiceSpy.getUserVideos.mockClear();

            // Still processing after one tick — keeps polling
            vi.advanceTimersByTime(3000);
            expect(videoServiceSpy.getUserVideos).toHaveBeenCalledTimes(1);

            // Now READY — polling should stop
            videoServiceSpy.getUserVideos.mockReturnValue(of([makeVideo(1, 'V1', 'READY')]));
            vi.advanceTimersByTime(3000);
            expect(videoServiceSpy.getUserVideos).toHaveBeenCalledTimes(2);

            vi.advanceTimersByTime(9000);
            expect(videoServiceSpy.getUserVideos).toHaveBeenCalledTimes(2);
        });

        it('should not poll when nothing is processing', () => {
            fixture.detectChanges(); // mockVideos are all READY
            videoServiceSpy.getUserVideos.mockClear();

            vi.advanceTimersByTime(10000);
            expect(videoServiceSpy.getUserVideos).not.toHaveBeenCalled();
        });

        it('should stop polling on destroy', () => {
            videoServiceSpy.getUserVideos.mockReturnValue(
                of([makeVideo(1, 'V1', 'PROCESSING')])
            );
            fixture.detectChanges();
            videoServiceSpy.getUserVideos.mockClear();

            fixture.destroy();
            vi.advanceTimersByTime(10000);
            expect(videoServiceSpy.getUserVideos).not.toHaveBeenCalled();
        });
    });
});
