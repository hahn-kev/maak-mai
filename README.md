# Bookmark Tags — Android app

[<img src="docs/images/badge_obtainium.png" alt="Get it on Obtainium" height="80">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22org.hahn.maakmai%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fhahn-kev%2Fmaak-mai%22%2C%22author%22%3A%22hahn-kev%22%2C%22name%22%3A%22Maak%20Mai%22%7D)

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
- Update applicationId and minSdk in app module’s build.gradle(.kts). versionCode/versionName come from the `VERSION_CODE`/`VERSION_NAME` environment variables (CI sets them on release; local builds default to 1 / "1.0").
- Set your preferred compileOptions and Kotlin JVM target for JDK 21.

## Releases and signing

Install the app from the [GitHub Releases](https://github.com/hahn-kev/maak-mai/releases) page, or add it to [Obtainium](https://github.com/ImranR98/Obtainium) with the badge at the top of this README so it picks up new releases automatically.

To publish a release, push a tag starting with `v`:

```bash
git tag v1.2.0
git push origin v1.2.0
```

CI runs the tests, builds a release APK named `maak-mai-<tag>.apk`, and attaches it to a new GitHub Release with auto-generated notes. The tag becomes the versionName and the CI run number becomes the versionCode, so each release installs as an upgrade.

Debug and release builds are both signed with `app/signing/maakmai.keystore`, which is committed to the repo on purpose. Builds from any machine or from CI then share one signature and install over each other without losing data. Because the key is public, anyone can sign an APK that installs over this app, so install APKs only from the Releases page.

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