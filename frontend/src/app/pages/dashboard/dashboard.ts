import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { VideoService, VideoInfo } from '../../services/video.service';
import { VideoCardComponent } from '../../components/video-card/video-card';
import { UploadDialogComponent } from '../../components/upload-dialog/upload-dialog';

@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [CommonModule, VideoCardComponent, UploadDialogComponent],
    templateUrl: './dashboard.html',
    styleUrl: './dashboard.scss'
})
export class DashboardComponent implements OnInit, OnDestroy {
    private static readonly POLL_INTERVAL_MS = 3000;

    videos: VideoInfo[] = [];
    loading = true;
    showUploadDialog = false;

    private pollTimer: ReturnType<typeof setInterval> | null = null;

    constructor(private videoService: VideoService, private router: Router) { }

    ngOnInit(): void {
        this.loadVideos();
    }

    ngOnDestroy(): void {
        this.stopPolling();
    }

    loadVideos(): void {
        this.loading = true;
        this.videoService.getUserVideos().subscribe({
            next: (videos) => {
                this.videos = videos;
                this.loading = false;
                this.syncPolling();
            },
            error: () => {
                this.loading = false;
            }
        });
    }

    openUpload(): void {
        this.showUploadDialog = true;
    }

    closeUpload(): void {
        this.showUploadDialog = false;
    }

    onUploaded(): void {
        this.showUploadDialog = false;
        this.loadVideos();
    }

    playVideo(id: number): void {
        this.router.navigate(['/play', id]);
    }

    deleteVideo(id: number): void {
        this.videoService.deleteVideo(id).subscribe({
            next: () => {
                this.videos = this.videos.filter(v => v.id !== id);
                this.syncPolling();
            }
        });
    }

    /** Poll while any video is still processing so thumbnails/duration appear. */
    private syncPolling(): void {
        const anyProcessing = this.videos.some(v => v.status === 'PROCESSING');
        if (anyProcessing && this.pollTimer === null) {
            this.pollTimer = setInterval(() => this.refreshSilently(), DashboardComponent.POLL_INTERVAL_MS);
        } else if (!anyProcessing) {
            this.stopPolling();
        }
    }

    private refreshSilently(): void {
        this.videoService.getUserVideos().subscribe({
            next: (videos) => {
                this.videos = videos;
                this.syncPolling();
            }
        });
    }

    private stopPolling(): void {
        if (this.pollTimer !== null) {
            clearInterval(this.pollTimer);
            this.pollTimer = null;
        }
    }
}
