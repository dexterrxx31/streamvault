import { Component, OnInit } from '@angular/core';
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
export class DashboardComponent implements OnInit {
    videos: VideoInfo[] = [];
    loading = true;
    showUploadDialog = false;

    constructor(private videoService: VideoService, private router: Router) { }

    ngOnInit(): void {
        this.loadVideos();
    }

    loadVideos(): void {
        this.loading = true;
        this.videoService.getUserVideos().subscribe({
            next: (videos) => {
                this.videos = videos;
                this.loading = false;
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
            }
        });
    }
}
