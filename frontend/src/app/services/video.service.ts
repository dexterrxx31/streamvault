import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType, HttpRequest } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface VideoInfo {
    id: number;
    title: string;
    contentType: string;
    size: number;
    uploadDate: string;
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
}
