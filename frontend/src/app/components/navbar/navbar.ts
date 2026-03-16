import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService, UserInfo } from '../../services/auth.service';

@Component({
    selector: 'app-navbar',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './navbar.html',
    styleUrl: './navbar.scss'
})
export class NavbarComponent {
    user: UserInfo | null = null;

    constructor(private authService: AuthService, private router: Router) {
        this.authService.currentUser$.subscribe(u => this.user = u);
    }

    logout(): void {
        this.authService.logout();
        this.router.navigate(['/']);
    }
}
