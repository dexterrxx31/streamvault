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

function makeVideo(overrides: Partial<VideoInfo> = {}): VideoInfo {
    return {
        id: 1,
        title: 'Video 1',
        contentType: 'video/mp4',
        size: 100,
        uploadDate: '2024-01-01',
        durationSeconds: 60,
        width: 1920,
        height: 1080,
        description: null,
        tags: [],
        status: 'READY',
        hasThumbnail: true,
        aiTitle: null,
        aiDescription: null,
        aiTags: [],
        streamUrl: '/api/videos/stream/1?exp=1&sig=s',
        thumbnailUrl: null,
        captionsUrl: null,
        ...overrides,
    };
}

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
                makeVideo({ id: 1, title: 'Video 1' }),
                makeVideo({ id: 2, title: 'Video 2', status: 'PROCESSING', hasThumbnail: false })
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

    describe('getVideo', () => {
        it('should return a single video', () => {
            const mockVideo = makeVideo({ id: 5, title: 'Single' });

            service.getVideo(5).subscribe(video => {
                expect(video).toEqual(mockVideo);
            });

            const req = httpMock.expectOne('http://localhost:8080/api/videos/5');
            expect(req.request.method).toBe('GET');
            req.flush(mockVideo);
        });
    });

    describe('updateVideo', () => {
        it('should send PUT with payload', () => {
            const updated = makeVideo({ title: 'New Title', tags: ['a'] });

            service.updateVideo(1, { title: 'New Title', tags: ['a'] }).subscribe(video => {
                expect(video.title).toBe('New Title');
            });

            const req = httpMock.expectOne('http://localhost:8080/api/videos/1');
            expect(req.request.method).toBe('PUT');
            expect(req.request.body).toEqual({ title: 'New Title', tags: ['a'] });
            req.flush(updated);
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

    describe('mediaUrl', () => {
        it('should prefix a signed media path with the API origin', () => {
            const url = service.mediaUrl('/api/videos/stream/5?exp=1&sig=abc');
            expect(url).toBe('http://localhost:8080/api/videos/stream/5?exp=1&sig=abc');
        });
    });

    describe('uploadVideo', () => {
        it('should report progress and finish', () => {
            const mockFile = new File([''], 'test.mp4', { type: 'video/mp4' });
            const mockVideo = makeVideo({ title: 'Test', size: 0, status: 'PROCESSING', hasThumbnail: false });

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
