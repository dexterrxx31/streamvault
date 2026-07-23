import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType, HttpRequest } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export type VideoStatus = 'PROCESSING' | 'READY' | 'FAILED';

export interface ChapterInfo {
    startSeconds: number;
    title: string;
}

export interface VideoInfo {
    id: number;
    title: string;
    contentType: string;
    size: number;
    uploadDate: string;
    durationSeconds: number | null;
    width: number | null;
    height: number | null;
    description: string | null;
    tags: string[];
    status: VideoStatus;
    hasThumbnail: boolean;
    aiTitle: string | null;
    aiDescription: string | null;
    aiTags: string[];
    summary: string | null;
    chapters: ChapterInfo[];
    hasCaptions: boolean;
}

export interface UpdateVideoPayload {
    title?: string;
    description?: string;
    tags?: string[];
}

export interface UploadProgress {
    progress: number;
    done: boolean;
    video?: VideoInfo;
}

@Injectable({ providedIn: 'root' })
export class VideoService {
    private readonly API_URL = 'http://localhost:8080/api/videos';

    constructor(private http: HttpClient) { }

    getUserVideos(): Observable<VideoInfo[]> {
        return this.http.get<VideoInfo[]>(this.API_URL);
    }

    getVideo(id: number): Observable<VideoInfo> {
        return this.http.get<VideoInfo>(`${this.API_URL}/${id}`);
    }

    updateVideo(id: number, payload: UpdateVideoPayload): Observable<VideoInfo> {
        return this.http.put<VideoInfo>(`${this.API_URL}/${id}`, payload);
    }

    uploadVideo(file: File, title: string): Observable<UploadProgress> {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('title', title);

        const req = new HttpRequest('POST', `${this.API_URL}/upload`, formData, {
            reportProgress: true,
        });

        return this.http.request(req).pipe(
            map((event: HttpEvent<any>) => {
                switch (event.type) {
                    case HttpEventType.UploadProgress:
                        const progress = event.total
                            ? Math.round((100 * event.loaded) / event.total)
                            : 0;
                        return { progress, done: false };
                    case HttpEventType.Response:
                        return { progress: 100, done: true, video: event.body as VideoInfo };
                    default:
                        return { progress: 0, done: false };
                }
            })
        );
    }

    deleteVideo(id: number): Observable<any> {
        return this.http.delete(`${this.API_URL}/${id}`);
    }

    getStreamUrl(id: number): string {
        return `${this.API_URL}/stream/${id}`;
    }

    getThumbnailUrl(id: number): string {
        return `${this.API_URL}/${id}/thumbnail`;
    }

    getCaptionsUrl(id: number): string {
        return `${this.API_URL}/${id}/captions.vtt`;
    }
}
