# Bookmark Tags — Android app

A simple, fast bookmark-keeping app where every bookmark can have multiple tags, and folders are “smart folders” that automatically include all bookmarks matching their tag.

- Bookmarks: title, URL, optional notes
- Tags: many-to-many with bookmarks
- Folders: one folder = one tag; a folder always shows bookmarks that have that tag

## Highlights

- Add, edit, delete bookmarks with tags
- Create tag-backed folders that auto-filter bookmarks
- Search and sort within folders
- Offline, local-first data
- Keyboard-friendly and accessible UI

## Tech stack

- Language: Kotlin 2.1
- Android SDK target: 36
- Java SDK: 21
- Architecture: MVVM with coroutines/Flow
- Persistence: Room (SQLite)
- Dependency Injection: Hilt
- UI: Jetpack Compose

## Core concepts

- Tags: Flexible labels you can assign to any bookmark.
- Smart folders: Each folder is bound to a single tag and shows all bookmarks containing that tag. Rename or delete the tag, and the folder updates automatically.
- Zero duplication: A bookmark can appear in many folders via its tags without copying data.

## Getting started

Prerequisites:
- Android Studio 2025.1.2 (Narwhal Feature Drop) or newer
- JDK 21
- Android SDK Platform 36
- Kotlin 2.1

Setup:
1. Clone the repository.
2. Open the project in Android Studio.
3. Let Gradle sync and download dependencies.
4. Select a device or emulator and click Run.

Configuration (optional):
- Update applicationId, minSdk, versionCode/Name in app module’s build.gradle(.kts).
- Set your preferred compileOptions and Kotlin JVM target for JDK 21.

## Project structure

- app/src/main/java/org/hahn/maakmai/
    - data/ Room entities, DAOs, repositories
    - model/ models, use-cases
    - di/ dependency injection setup

## Typical flows

- Create a bookmark: enter title + URL, optionally notes and tags.
- Create a folder: the name of the folder is the tag, the folder shows all bookmarks with that tag.
- Browse: open any folder to see its matching bookmarks. Search/sort inside.

## Testing

- Unit tests for:
    - Tag parsing/normalization
    - Bookmark-tag relationships
    - Folder filtering rules
- Instrumented tests for:
    - DAO queries (Room)
    - UI list filtering and state restoration

## Roadmap ideas

- Import/export bookmarks (JSON)
- Share-to-app from browsers
- Duplicate detection and URL normalization
- Sorting by date/title/domain
- Multi-select for bulk tag edits
- Theming and dark mode

## Contributing

- Open an issue describing the change or bug.
- For features, include UI/UX notes and edge cases.
- Submit a PR with tests where applicable.

## License

MIT, feel free to use it however you want

Questions or suggestions? Please open an issue.