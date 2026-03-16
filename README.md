# StreamVault

StreamVault is a full-stack video streaming platform built with **Angular** and **Spring Boot**.

## Features

- **User Authentication**: Secure signup and login with JWT.
- **Video Upload**: Upload video files with progress tracking.
- **Video Streaming**: Stream uploaded videos directly in the browser.
- **Dashboard**: View and manage your uploaded videos.
- **Player**: Dedicated player page for immersive viewing.

## Tech Stack

- **Frontend**: Angular (Standalone Components), RxJS, CSS/SCSS.
- **Backend**: Java Spring Boot, Spring Security, JPA/Hibernate.
- **Database**: H2 (In-memory).
- **Storage**: Local file system.
- **Testing**:
  - Frontend: Vitest + AnalogJS.
  - Backend: JUnit 5 + Mockito.

## Getting Started

### Prerequisites

- Node.js & npm
- JDK 17+
- Maven

### Running the Backend

1. Navigate to `backend/`.
2. Run `./mvnw spring-boot:run`.
3. The API will be available at `http://localhost:8080`.

### Running the Frontend

1. Navigate to `frontend/`.
2. Run `npm install` (first time).
3. Run `npm start` or `npm run dev`.
4. Open `http://localhost:4200` in your browser.

## Testing

### Frontend Tests
Run `npm test` in the `frontend/` directory.

### Backend Tests
Run `./mvnw test` in the `backend/` directory.

## UI Design
The application features a modern, premium design with:
- Glassmorphism effects
- Responsive layouts
- Smooth fade-in animations
- Integrated progress indicators
