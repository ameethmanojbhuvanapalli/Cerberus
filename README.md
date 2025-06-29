# Cerberus

Cerberus is an Android application for centralized authentication management, built using Kotlin and Java. It leverages an event-driven architecture to handle authentication events efficiently and securely.

## Features

- Centralized authentication for multiple apps
- Event-driven callback system for authentication events
- Idle timeout and session management
- Dynamic authenticator switching
- Thread-safe authentication state handling

## Architecture

Cerberus is designed with the following software patterns:

- **Singleton Pattern:** Ensures a single instance of `AuthenticationService` throughout the app lifecycle.
- **Observer Pattern:** Uses callbacks to notify registered listeners about authentication events.
- **Event-Driven Architecture:** Decouples authentication logic from UI and other components, making the system modular and maintainable.

  ## Requirements
  
- **Android Studio**: Meerkat Feature Drop | 2024.3.2 Patch 1 or later
- **JDK**: 17 or later
- **Gradle**: 8.0 or later (handled automatically by Android Studio)
- **Minimum Android SDK**: 33 (Android 13)
- **Kotlin**: 1.8 or later
- **Java**: 8 or later

## How It Works

1. Apps request authentication via the `AuthenticationService`.
2. The service checks if the app is already authenticated or if re-authentication is required.
3. Authentication events (success/failure) are propagated to registered callbacks.
4. Idle timeouts are managed per app, with automatic cleanup of expired sessions.

## Project Structure

- `app/src/main/java/com/example/cerberus/service/AuthenticationService.kt` — Core authentication logic
- `app/src/main/java/com/example/cerberus/auth/` — Authentication interfaces and callback definitions
- `app/src/main/java/com/example/cerberus/service/AppLockService.kt` \— App lock and session management
  
## Getting Started

1. Clone the repository.
2. Open the project in Android Studio.
3. Build and run the app on an emulator or device.

## Build

This project uses Gradle. To build from the command line:

```sh
./gradlew build
