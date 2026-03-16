import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterLink],
    templateUrl: './login.html',
    styleUrl: './login.scss'
})
export class LoginComponent {
    username = '';
    password = '';
    error = '';
    loading = false;

    constructor(private authService: AuthService, private router: Router) { }

    onSubmit(): void {
        if (!this.username || !this.password) {
            this.error = 'Please fill in all fields';
            return;
        }

        this.loading = true;
        this.error = '';

        this.authService.login(this.username, this.password).subscribe({
            next: () => {
                this.router.navigate(['/dashboard']);
            },
            error: (err) => {
                this.loading = false;
                this.error = err.error?.error || 'Login failed. Please try again.';
            }
        });
    }
}
