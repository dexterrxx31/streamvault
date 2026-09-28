import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { VideoInfo, VideoService } from '../../services/video.service';

@Component({
    selector: 'app-video-card',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './video-card.html',
    styleUrl: './video-card.scss'
})
export class VideoCardComponent {
    @Input() video!: VideoInfo;
    @Output() play = new EventEmitter<number>();
    @Output() delete = new EventEmitter<number>();

    thumbnailFailed = false;

    constructor(private videoService: VideoService) { }

    get showThumbnail(): boolean {
        return this.video.status === 'READY' && !!this.video.thumbnailUrl && !this.thumbnailFailed;
    }

    get thumbnailUrl(): string {
        return this.video.thumbnailUrl ? this.videoService.mediaUrl(this.video.thumbnailUrl) : '';
    }

    onThumbnailError(): void {
        this.thumbnailFailed = true;
    }

    onPlay(): void {
        this.play.emit(this.video.id);
    }

    onDelete(event: Event): void {
        event.stopPropagation();
        if (confirm('Are you sure you want to delete this video?')) {
            this.delete.emit(this.video.id);
        }
    }

    formatSize(bytes: number): string {
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
        return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
    }

    formatDate(dateStr: string): string {
        const date = new Date(dateStr);
        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    }

    formatDuration(seconds: number): string {
        const total = Math.round(seconds);
        const h = Math.floor(total / 3600);
        const m = Math.floor((total % 3600) / 60);
        const s = total % 60;
        const pad = (n: number) => n.toString().padStart(2, '0');
        return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`;
    }
}
