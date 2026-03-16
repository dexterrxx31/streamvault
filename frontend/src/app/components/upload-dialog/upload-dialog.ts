import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VideoService, UploadProgress } from '../../services/video.service';

@Component({
    selector: 'app-upload-dialog',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './upload-dialog.html',
    styleUrl: './upload-dialog.scss'
})
export class UploadDialogComponent {
    @Output() close = new EventEmitter<void>();
    @Output() uploaded = new EventEmitter<void>();

    selectedFile: File | null = null;
    title = '';
    progress = 0;
    uploading = false;
    error = '';
    dragOver = false;

    constructor(private videoService: VideoService) { }

    onDragOver(event: DragEvent): void {
        event.preventDefault();
        this.dragOver = true;
    }

    onDragLeave(event: DragEvent): void {
        event.preventDefault();
        this.dragOver = false;
    }

    onDrop(event: DragEvent): void {
        event.preventDefault();
        this.dragOver = false;
        const files = event.dataTransfer?.files;
        if (files && files.length > 0) {
            this.selectFile(files[0]);
        }
    }

    onFileSelect(event: Event): void {
        const input = event.target as HTMLInputElement;
        if (input.files && input.files.length > 0) {
            this.selectFile(input.files[0]);
        }
    }

    private selectFile(file: File): void {
        if (!file.type.startsWith('video/')) {
            this.error = 'Please select a video file';
            return;
        }
        this.selectedFile = file;
        if (!this.title) {
            this.title = file.name.replace(/\.[^/.]+$/, '');
        }
        this.error = '';
    }

    formatSize(bytes: number): string {
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    }

    onUpload(): void {
        if (!this.selectedFile) {
            this.error = 'Please select a video file';
            return;
        }
        if (!this.title.trim()) {
            this.error = 'Please enter a title';
            return;
        }

        this.uploading = true;
        this.error = '';
        this.progress = 0;

        this.videoService.uploadVideo(this.selectedFile, this.title.trim()).subscribe({
            next: (event: UploadProgress) => {
                this.progress = event.progress;
                if (event.done) {
                    this.uploading = false;
                    this.uploaded.emit();
                }
            },
            error: (err) => {
                this.uploading = false;
                this.error = err.error?.error || 'Upload failed. Please try again.';
            }
        });
    }

    onClose(): void {
        if (!this.uploading) {
            this.close.emit();
        }
    }

    onBackdropClick(event: MouseEvent): void {
        if ((event.target as HTMLElement).classList.contains('upload-overlay')) {
            this.onClose();
        }
    }
}
