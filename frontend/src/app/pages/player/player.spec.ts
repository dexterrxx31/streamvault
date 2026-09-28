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
            getVideo: vi.fn().mockReturnValue(of({
                id: 1,
                title: 'Test Video',
                contentType: 'video/mp4',
                size: 1024,
                uploadDate: '2024-03-15',
                durationSeconds: 120,
                width: 1920,
                height: 1080,
                description: null,
                tags: [],
                status: 'READY',
                hasThumbnail: true,
                aiTitle: null,
                aiDescription: null,
                aiTags: [],
                summary: null,
                chapters: [],
                hasCaptions: false,
                streamUrl: '/api/videos/stream/1?exp=1&sig=s',
                thumbnailUrl: null,
                captionsUrl: null
            })),
            updateVideo: vi.fn(),
            mediaUrl: vi.fn((path: string) => 'http://api' + path)
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
        expect(component.streamUrl).toBe('http://api/api/videos/stream/1?exp=1&sig=s');
        expect(component.captionsUrl).toBe('');
    });

    it('should navigate back to dashboard', () => {
        fixture.detectChanges();
        component.goBack();
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
    });

    it('should not show AI suggestions banner without suggestions', () => {
        fixture.detectChanges();
        expect(component.hasSuggestions).toBe(false);
        expect(fixture.nativeElement.querySelector('.ai-suggestions')).toBeFalsy();
    });

    describe('AI suggestions', () => {
        const suggested = {
            id: 1, title: 'Test Video', contentType: 'video/mp4', size: 1024,
            uploadDate: '2024-03-15', durationSeconds: 120, width: 1920, height: 1080,
            description: null, tags: [], status: 'READY', hasThumbnail: true,
            aiTitle: 'AI Better Title', aiDescription: 'AI description', aiTags: ['fun', 'demo'],
            summary: null, chapters: [], hasCaptions: false
        };
        const resolved = { ...suggested, title: 'AI Better Title', aiTitle: null, aiDescription: null, aiTags: [] };

        beforeEach(() => {
            videoServiceSpy.getVideo.mockReturnValue(of(suggested));
        });

        it('should show the suggestions banner', () => {
            fixture.detectChanges();
            expect(component.hasSuggestions).toBe(true);
            const banner = fixture.nativeElement.querySelector('.ai-suggestions');
            expect(banner.textContent).toContain('AI Better Title');
            expect(banner.textContent).toContain('AI description');
            expect(banner.textContent).toContain('fun');
        });

        it('should apply suggested values on accept', () => {
            videoServiceSpy.updateVideo.mockReturnValue(of(resolved));
            fixture.detectChanges();

            component.acceptSuggestions();

            expect(videoServiceSpy.updateVideo).toHaveBeenCalledWith(1, {
                title: 'AI Better Title',
                description: 'AI description',
                tags: ['fun', 'demo']
            });
            expect(component.video?.title).toBe('AI Better Title');
            expect(component.hasSuggestions).toBe(false);
        });

        it('should clear suggestions without changes on dismiss', () => {
            videoServiceSpy.updateVideo.mockReturnValue(of({ ...resolved, title: 'Test Video' }));
            fixture.detectChanges();

            component.dismissSuggestions();

            expect(videoServiceSpy.updateVideo).toHaveBeenCalledWith(1, {});
            expect(component.video?.title).toBe('Test Video');
            expect(component.hasSuggestions).toBe(false);
        });
    });

    describe('captions and chapters', () => {
        const withChapters = {
            id: 1, title: 'Test Video', contentType: 'video/mp4', size: 1024,
            uploadDate: '2024-03-15', durationSeconds: 120, width: 1920, height: 1080,
            description: null, tags: [], status: 'READY', hasThumbnail: true,
            aiTitle: null, aiDescription: null, aiTags: [],
            summary: 'A short walkthrough of the app.',
            chapters: [
                { startSeconds: 0, title: 'Intro' },
                { startSeconds: 65, title: 'Main topic' }
            ],
            hasCaptions: true,
            streamUrl: '/api/videos/stream/1?exp=1&sig=s',
            thumbnailUrl: null,
            captionsUrl: '/api/videos/1/captions.vtt?exp=1&sig=c'
        };

        beforeEach(() => {
            videoServiceSpy.getVideo.mockReturnValue(of(withChapters));
        });

        it('should render captions track when available', () => {
            fixture.detectChanges();
            const track = fixture.nativeElement.querySelector('track');
            expect(track).toBeTruthy();
            expect(track.getAttribute('src')).toBe('http://api/api/videos/1/captions.vtt?exp=1&sig=c');
        });

        it('should render summary and clickable chapters', () => {
            fixture.detectChanges();
            expect(fixture.nativeElement.querySelector('.video-summary').textContent)
                .toContain('A short walkthrough of the app.');

            const items = fixture.nativeElement.querySelectorAll('.chapter-item');
            expect(items.length).toBe(2);
            expect(items[1].textContent).toContain('1:05');
            expect(items[1].textContent).toContain('Main topic');
        });

        it('should seek the video element on chapter click', () => {
            fixture.detectChanges();
            const videoEl = fixture.nativeElement.querySelector('video');
            videoEl.play = vi.fn();

            const items = fixture.nativeElement.querySelectorAll('.chapter-item');
            items[1].click();

            expect(videoEl.currentTime).toBe(65);
            expect(videoEl.play).toHaveBeenCalled();
        });

        it('should format chapter timestamps', () => {
            expect(component.formatChapterTime(0)).toBe('0:00');
            expect(component.formatChapterTime(65)).toBe('1:05');
            expect(component.formatChapterTime(3671)).toBe('1:01:11');
        });
    });
});
