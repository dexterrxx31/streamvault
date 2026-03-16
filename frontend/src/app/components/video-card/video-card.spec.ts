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
import { By } from '@angular/platform-browser';

describe('VideoCardComponent', () => {
    let component: VideoCardComponent;
    let fixture: ComponentFixture<VideoCardComponent>;

    const mockVideo = {
        id: 1,
        title: 'Test Video Name',
        contentType: 'video/mp4',
        size: 1024 * 1024 * 5, // 5MB
        uploadDate: '2024-03-15T10:00:00Z'
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [VideoCardComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(VideoCardComponent);
        component = fixture.componentInstance;
        component.video = mockVideo;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should display video title', () => {
        const titleEl = fixture.debugElement.query(By.css('.card-title')).nativeElement;
        expect(titleEl.textContent).toBe('Test Video Name');
    });

    it('should format size correctly', () => {
        expect(component.formatSize(1024)).toBe('1.0 KB');
        expect(component.formatSize(1024 * 1024)).toBe('1.0 MB');
        expect(component.formatSize(1024 * 1024 * 1024)).toBe('1.00 GB');
    });

    it('should emit play event when clicked', () => {
        vi.spyOn(component.play, 'emit');
        const cardDe = fixture.debugElement.query(By.css('.video-card'));
        cardDe.nativeElement.click();
        expect(component.play.emit).toHaveBeenCalledWith(1);
    });

    it('should emit delete event when delete button clicked and confirmed', () => {
        vi.spyOn(component.delete, 'emit');
        vi.spyOn(window, 'confirm').mockReturnValue(true);

        const deleteBtn = fixture.debugElement.query(By.css('.card-delete'));
        deleteBtn.nativeElement.click();

        expect(window.confirm).toHaveBeenCalled();
        expect(component.delete.emit).toHaveBeenCalledWith(1);
    });

    it('should NOT emit delete event if not confirmed', () => {
        vi.spyOn(component.delete, 'emit');
        vi.spyOn(window, 'confirm').mockReturnValue(false);

        const deleteBtn = fixture.debugElement.query(By.css('.card-delete'));
        deleteBtn.nativeElement.click();

        expect(component.delete.emit).not.toHaveBeenCalled();
    });
});
