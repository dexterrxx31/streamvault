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
import { VideoCardComponent } from './video-card';
import { VideoService, VideoInfo } from '../../services/video.service';
import { By } from '@angular/platform-browser';

describe('VideoCardComponent', () => {
    let component: VideoCardComponent;
    let fixture: ComponentFixture<VideoCardComponent>;

    const mockVideo: VideoInfo = {
        id: 1,
        title: 'Test Video Name',
        contentType: 'video/mp4',
        size: 1024 * 1024 * 5, // 5MB
        uploadDate: '2024-03-15T10:00:00Z',
        durationSeconds: 125,
        width: 1920,
        height: 1080,
        description: null,
        tags: [],
        status: 'READY',
        hasThumbnail: true
    };

    beforeEach(async () => {
        const videoServiceSpy = {
            getThumbnailUrl: vi.fn().mockReturnValue('http://thumb/1')
        };

        await TestBed.configureTestingModule({
            imports: [VideoCardComponent],
            providers: [{ provide: VideoService, useValue: videoServiceSpy }]
        }).compileComponents();

        fixture = TestBed.createComponent(VideoCardComponent);
        component = fixture.componentInstance;
        component.video = { ...mockVideo };
    });

    /** Render once with the given state — jsdom fires async <img> error events,
     *  so tests set state up front instead of mutating after a first render. */
    function render(overrides: Partial<VideoInfo> = {}): void {
        component.video = { ...mockVideo, ...overrides };
        fixture.detectChanges();
    }

    it('should create', () => {
        render();
        expect(component).toBeTruthy();
    });

    it('should display video title', () => {
        render();
        const titleEl = fixture.debugElement.query(By.css('.card-title')).nativeElement;
        expect(titleEl.textContent).toBe('Test Video Name');
    });

    it('should format size correctly', () => {
        expect(component.formatSize(1024)).toBe('1.0 KB');
        expect(component.formatSize(1024 * 1024)).toBe('1.0 MB');
        expect(component.formatSize(1024 * 1024 * 1024)).toBe('1.00 GB');
    });

    it('should format duration correctly', () => {
        expect(component.formatDuration(65)).toBe('1:05');
        expect(component.formatDuration(3671)).toBe('1:01:11');
        expect(component.formatDuration(9)).toBe('0:09');
    });

    it('should show thumbnail image with duration badge when READY', () => {
        render();
        const img = fixture.debugElement.query(By.css('.thumbnail-image'));
        expect(img).toBeTruthy();
        expect(img.nativeElement.src).toBe('http://thumb/1');

        const badge = fixture.debugElement.query(By.css('.duration-badge'));
        expect(badge.nativeElement.textContent.trim()).toBe('2:05');
    });

    it('should fall back to placeholder when thumbnail fails to load', () => {
        component.onThumbnailError();
        render();

        expect(fixture.debugElement.query(By.css('.thumbnail-image'))).toBeFalsy();
        expect(fixture.debugElement.query(By.css('.thumbnail-placeholder'))).toBeTruthy();
    });

    it('should show processing badge and no thumbnail while PROCESSING', () => {
        render({ status: 'PROCESSING', hasThumbnail: false, durationSeconds: null });

        expect(fixture.debugElement.query(By.css('.thumbnail-image'))).toBeFalsy();
        const badge = fixture.debugElement.query(By.css('.status-badge.processing'));
        expect(badge.nativeElement.textContent).toContain('Processing');
    });

    it('should show failed badge when FAILED', () => {
        render({ status: 'FAILED' });

        const badge = fixture.debugElement.query(By.css('.status-badge.failed'));
        expect(badge.nativeElement.textContent).toContain('Processing failed');
    });

    it('should emit play event when clicked', () => {
        render();
        vi.spyOn(component.play, 'emit');
        const cardDe = fixture.debugElement.query(By.css('.video-card'));
        cardDe.nativeElement.click();
        expect(component.play.emit).toHaveBeenCalledWith(1);
    });

    it('should emit delete event when delete button clicked and confirmed', () => {
        render();
        vi.spyOn(component.delete, 'emit');
        vi.spyOn(window, 'confirm').mockReturnValue(true);

        const deleteBtn = fixture.debugElement.query(By.css('.card-delete'));
        deleteBtn.nativeElement.click();

        expect(window.confirm).toHaveBeenCalled();
        expect(component.delete.emit).toHaveBeenCalledWith(1);
    });

    it('should NOT emit delete event if not confirmed', () => {
        render();
        vi.spyOn(component.delete, 'emit');
        vi.spyOn(window, 'confirm').mockReturnValue(false);

        const deleteBtn = fixture.debugElement.query(By.css('.card-delete'));
        deleteBtn.nativeElement.click();

        expect(component.delete.emit).not.toHaveBeenCalled();
    });
});
