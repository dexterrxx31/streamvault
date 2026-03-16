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

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { VideoService, VideoInfo } from './video.service';
import { HttpEventType } from '@angular/common/http';

describe('VideoService', () => {
    let service: VideoService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [VideoService]
        });
        service = TestBed.inject(VideoService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('getUserVideos', () => {
        it('should return user videos', () => {
            const mockVideos: VideoInfo[] = [
                { id: 1, title: 'Video 1', contentType: 'video/mp4', size: 100, uploadDate: '2024-01-01' },
                { id: 2, title: 'Video 2', contentType: 'video/mp4', size: 200, uploadDate: '2024-01-02' }
            ];

            service.getUserVideos().subscribe(videos => {
                expect(videos.length).toBe(2);
                expect(videos).toEqual(mockVideos);
            });

            const req = httpMock.expectOne('http://localhost:8080/api/videos');
            expect(req.request.method).toBe('GET');
            req.flush(mockVideos);
        });
    });

    describe('deleteVideo', () => {
        it('should call delete endpoint', () => {
            service.deleteVideo(1).subscribe();

            const req = httpMock.expectOne('http://localhost:8080/api/videos/1');
            expect(req.request.method).toBe('DELETE');
            req.flush({});
        });
    });

    describe('getStreamUrl', () => {
        it('should return correct stream URL', () => {
            const url = service.getStreamUrl(5);
            expect(url).toBe('http://localhost:8080/api/videos/stream/5');
        });
    });

    describe('uploadVideo', () => {
        it('should report progress and finish', () => {
            const mockFile = new File([''], 'test.mp4', { type: 'video/mp4' });
            const mockVideo: VideoInfo = { id: 1, title: 'Test', contentType: 'video/mp4', size: 0, uploadDate: '' };

            let states: any[] = [];
            service.uploadVideo(mockFile, 'Test').subscribe(state => states.push(state));

            const req = httpMock.expectOne('http://localhost:8080/api/videos/upload');
            expect(req.request.method).toBe('POST');

            // Simulate progress
            req.event({
                type: HttpEventType.UploadProgress,
                loaded: 50,
                total: 100
            });

            // Simulate completion
            req.event({
                type: HttpEventType.Response,
                body: mockVideo,
                status: 200,
                statusText: 'OK',
                ok: true,
                headers: {} as any,
                url: ''
            } as any);

            expect(states).toEqual([
                { progress: 0, done: false },
                { progress: 50, done: false },
                { progress: 100, done: true, video: mockVideo }
            ]);
        });
    });
});
