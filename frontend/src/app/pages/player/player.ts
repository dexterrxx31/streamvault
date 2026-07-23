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
    suggestionBusy = false;

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

    get hasSuggestions(): boolean {
        return !!(this.video && (this.video.aiTitle || this.video.aiDescription || this.video.aiTags.length));
    }

    acceptSuggestions(): void {
        if (!this.video) return;
        this.suggestionBusy = true;
        this.videoService.updateVideo(this.videoId, {
            title: this.video.aiTitle ?? undefined,
            description: this.video.aiDescription ?? undefined,
            tags: this.video.aiTags.length ? this.video.aiTags : undefined,
        }).subscribe({
            next: (video) => {
                this.video = video;
                this.suggestionBusy = false;
            },
            error: () => {
                this.suggestionBusy = false;
            }
        });
    }

    dismissSuggestions(): void {
        if (!this.video) return;
        this.suggestionBusy = true;
        // An empty update keeps current metadata but resolves the suggestions
        this.videoService.updateVideo(this.videoId, {}).subscribe({
            next: (video) => {
                this.video = video;
                this.suggestionBusy = false;
            },
            error: () => {
                this.suggestionBusy = false;
            }
        });
    }

    private loadVideoInfo(): void {
        this.videoService.getVideo(this.videoId).subscribe({
            next: (video) => {
                this.video = video;
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
