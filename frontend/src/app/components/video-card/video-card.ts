import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { VideoInfo } from '../../services/video.service';

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
}
