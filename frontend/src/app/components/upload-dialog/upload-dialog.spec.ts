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
import { UploadDialogComponent } from './upload-dialog';
import { VideoService } from '../../services/video.service';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';

describe('UploadDialogComponent', () => {
    let component: UploadDialogComponent;
    let fixture: ComponentFixture<UploadDialogComponent>;
    let videoServiceSpy: any;

    beforeEach(async () => {
        videoServiceSpy = {
            uploadVideo: vi.fn()
        };

        await TestBed.configureTestingModule({
            imports: [UploadDialogComponent, FormsModule],
            providers: [
                { provide: VideoService, useValue: videoServiceSpy }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(UploadDialogComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should show error if uploading without file', () => {
        component.onUpload();
        expect(component.error).toBe('Please select a video file');
    });

    it('should show error if uploading without title', () => {
        const file = new File([''], 'test.mp4', { type: 'video/mp4' });
        component.onFileSelect({ target: { files: [file] } } as any);
        component.title = '';
        component.onUpload();
        expect(component.error).toBe('Please enter a title');
    });

    it('should call upload and emit on success', () => {
        const file = new File([''], 'test.mp4', { type: 'video/mp4' });
        const progressEvents = [
            { progress: 50, done: false },
            { progress: 100, done: true }
        ];
        videoServiceSpy.uploadVideo.mockReturnValue(of(...progressEvents));
        vi.spyOn(component.uploaded, 'emit');

        component.onFileSelect({ target: { files: [file] } } as any);
        component.title = 'My Video';
        component.onUpload();

        expect(videoServiceSpy.uploadVideo).toHaveBeenCalledWith(file, 'My Video');
        expect(component.progress).toBe(100);
        expect(component.uploading).toBe(false);
        expect(component.uploaded.emit).toHaveBeenCalled();
    });

    it('should handle upload error', () => {
        const file = new File([''], 'test.mp4', { type: 'video/mp4' });
        videoServiceSpy.uploadVideo.mockReturnValue(throwError(() => ({ error: { error: 'Server error' } })));

        component.onFileSelect({ target: { files: [file] } } as any);
        component.title = 'My Video';
        component.onUpload();

        expect(component.uploading).toBe(false);
        expect(component.error).toBe('Server error');
    });

    it('should close dialog', () => {
        vi.spyOn(component.close, 'emit');
        component.onClose();
        expect(component.close.emit).toHaveBeenCalled();
    });
});
