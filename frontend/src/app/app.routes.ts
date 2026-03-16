import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
    {
        path: '',
        loadComponent: () => import('./pages/landing/landing').then(m => m.LandingComponent)
    },
    {
        path: 'login',
        loadComponent: () => import('./pages/login/login').then(m => m.LoginComponent)
    },
    {
        path: 'signup',
        loadComponent: () => import('./pages/signup/signup').then(m => m.SignupComponent)
    },
    {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/dashboard').then(m => m.DashboardComponent),
        canActivate: [authGuard]
    },
    {
        path: 'play/:id',
        loadComponent: () => import('./pages/player/player').then(m => m.PlayerComponent),
        canActivate: [authGuard]
    },
    {
        path: '**',
        redirectTo: ''
    }
];
