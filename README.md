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

## CI/CD and Signing

The project uses GitHub Actions for CI/CD. Production builds are automatically signed and released when a tag starting with `v` (e.g., `v1.0.0`) is pushed to the repository.

### Setting up Signing for CI

To enable signed production builds in CI, you must configure the following GitHub Secrets in your repository:

1.  `RELEASE_KEYSTORE_BASE64`: The base64-encoded content of your `.jks` or `.keystore` file.
    - You can generate this using: `base64 -w 0 your-keystore.jks`
2.  `RELEASE_STORE_PASSWORD`: The password for your keystore.
3.  `RELEASE_KEY_ALIAS`: The alias for your signing key.
4.  `RELEASE_KEY_PASSWORD`: The password for your signing key.

### How to Release

1.  Ensure all changes are merged into the `main` branch.
2.  Create and push a new tag:
    ```bash
    git tag v1.0.0
    git push origin v1.0.0
    ```
3.  The CI will trigger, build the signed APK, create a GitHub Release named `Release 1.0.0`, and upload the APK as an asset.
4.  The `versionCode` will be automatically set to the GitHub Action run number, and the `versionName` will be derived from the tag (e.g., `1.0.0`).

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