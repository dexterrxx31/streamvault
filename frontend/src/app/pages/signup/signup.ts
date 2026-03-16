import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
    selector: 'app-signup',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterLink],
    templateUrl: './signup.html',
    styleUrl: './signup.scss'
})
export class SignupComponent {
    username = '';
    email = '';
    password = '';
    confirmPassword = '';
    error = '';
    loading = false;

    constructor(private authService: AuthService, private router: Router) { }

    onSubmit(): void {
        if (!this.username || !this.email || !this.password || !this.confirmPassword) {
            this.error = 'Please fill in all fields';
            return;
        }

        if (this.password !== this.confirmPassword) {
            this.error = 'Passwords do not match';
            return;
        }

        if (this.password.length < 6) {
            this.error = 'Password must be at least 6 characters';
            return;
        }

        this.loading = true;
        this.error = '';

        this.authService.signup(this.username, this.email, this.password).subscribe({
            next: () => {
                this.router.navigate(['/dashboard']);
            },
            error: (err) => {
                this.loading = false;
                this.error = err.error?.error || 'Signup failed. Please try again.';
            }
        });
    }
}
