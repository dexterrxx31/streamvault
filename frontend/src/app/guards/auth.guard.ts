import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (authService.isLoggedIn) {
        return true;
    }

    // Clears a stale/expired token so the navbar doesn't show a logged-in user
    authService.logout();
    router.navigate(['/login']);
    return false;
};
