import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { VideoService, VideoInfo, ChapterInfo } from '../../services/video.service';

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
    captionsUrl: string = '';
    video: VideoInfo | null = null;
    loading = true;
    suggestionBusy = false;

    @ViewChild('videoRef') videoRef?: ElementRef<HTMLVideoElement>;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private videoService: VideoService
    ) { }

    ngOnInit(): void {
        this.route.params.subscribe(params => {
            this.videoId = +params['id'];
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
                // Signed URLs are set once; later metadata updates return fresh
                // signatures, and swapping <video src> would restart playback
                this.streamUrl = this.videoService.mediaUrl(video.streamUrl);
                this.captionsUrl = video.captionsUrl ? this.videoService.mediaUrl(video.captionsUrl) : '';
                this.loading = false;
            },
            error: () => {
                this.loading = false;
            }
        });
    }

    seekTo(chapter: ChapterInfo): void {
        const video = this.videoRef?.nativeElement;
        if (video) {
            video.currentTime = chapter.startSeconds;
            video.play();
        }
    }

    formatChapterTime(seconds: number): string {
        const total = Math.round(seconds);
        const h = Math.floor(total / 3600);
        const m = Math.floor((total % 3600) / 60);
        const s = total % 60;
        const pad = (n: number) => n.toString().padStart(2, '0');
        return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`;
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
