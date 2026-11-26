# Daymark project guide

- Native Android app using Kotlin, Jetpack Compose, Room, Glance and MVVM with a repository.
- `com.daymark.app.data` owns persistence, date models, and centralized task status/calendar business logic. `ui` is currently colocated in `MainActivity.kt`; Android integrations live in `widget` and `reminders`.
- Keep planned work dates and actual deadlines independent. An unfinished task whose plan date passed but deadline is still future is `MISSED_PLAN`, not `OVERDUE`. Completed tasks always calculate to `COMPLETED`.
- Store dates as ISO LocalDate and instants as epoch milliseconds. Never persist derived task status.
- Build: `gradle :app:assembleDebug`. Tests: `gradle :app:testDebugUnitTest`. Requires JDK 17 and Android SDK 36.
- Use the Gradle version catalog in `gradle/libs.versions.toml`. Room schema changes require real migrations; do not add destructive fallbacks.
