import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { VideoService, VideoInfo } from '../../services/video.service';

@Component({
    selector: 'app-player',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './player.html',
    styleUrl: './player.scss'
})
export class PlayerComponent implements OnInit {
    videoId: number = 0;
    streamUrl: string = '';
    video: VideoInfo | null = null;
    loading = true;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private videoService: VideoService
    ) { }

    ngOnInit(): void {
        this.route.params.subscribe(params => {
            this.videoId = +params['id'];
            this.streamUrl = this.videoService.getStreamUrl(this.videoId);
            this.loadVideoInfo();
        });
    }

    private loadVideoInfo(): void {
        this.videoService.getUserVideos().subscribe({
            next: (videos) => {
                this.video = videos.find(v => v.id === this.videoId) || null;
                this.loading = false;
            },
            error: () => {
                this.loading = false;
            }
        });
    }

    goBack(): void {
        this.router.navigate(['/dashboard']);
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
