# F95GM

F95GM (F95 Game Manager) is a local catalog and tracking application for the authenticated F95zone latest-alpha feed. It
gives you a focused way to browse releases, search and filter the catalog, open complete game details, and keep a
private list of games you are following.

F95GM is intended for adults. The application displays adult content and requires an F95zone account. You must confirm
that you are over 18 before signing in.

## What it does

- Browses the current F95zone latest-alpha catalog of games, comics, animations, assets, and mods.
- Searches by title or developer and filters by date, tags, prefixes, category, and sort order.
- Opens a local detail page for each release, including its title, developer, cover, metadata, overview, sections,
  images, and download links when available.
- Tracks games with states such as Watching, Downloading, Downloaded, Playable, Paused, and Completed.
- Checks tracked games for newer versions and keeps an update highlighted until you mark the version as downloaded.
- Saves named filter presets as private quick links. Presets can also be exported and imported when moving or
  reinstalling the application.
- Supports dark, light, and system themes, adjustable page/card sizes, lazy-loaded images, and configurable automatic
  update checks.
- Keeps F95zone session cookies, tracked games, and saved filters on the local machine. F95GM never stores your password
  and does not send tracking data or saved filters back to F95zone.

## Using F95GM

1. Start the application and wait for the local page to open in your browser.
2. Enter your F95zone username or email and password, confirm the 18+ notice, and choose **Sign in and open catalog**.
3. Browse the catalog. Use the filter panel to search, select tags or prefixes, set a date range, and change sorting.
   Apply the filters to refresh the results.
4. Select a card to open its details. From the detail page you can inspect the release and choose a private tracking
   state.
5. Open **Tracked games** to search your list, change a tracking state, remove a game, or choose **Check tracked games**
   to look for newer versions.
6. Use **Saved filters** in the catalog filter panel to name a filter. Its quick link appears beside the catalog
   navigation. Export or import these links from **Settings**.
7. Use **Settings** to change the theme, image loading, page size, card size, and automatic tracked-game update
   interval.

The browser session and upstream F95zone session are restored after a JVM restart and expire after 30 days. If F95zone
rejects the upstream session, F95GM clears the local session and asks you to sign in again. Signing out removes the
local browser session and persisted upstream session.

## Build and run from source

### Requirements

- Windows, macOS, or Linux
- A JDK 25 installation (the project configures the Kotlin JVM toolchain for Java 25)
- Internet access to download Gradle/dependencies and to reach `f95zone.to`

The repository includes the Gradle wrapper, so a separate Gradle installation is not required. On Windows, use
`gradlew.bat`; on macOS/Linux, use `./gradlew`.

### Run the complete application

From the repository root:

```powershell
.\gradlew.bat jvmRunApp
```

This builds the production Kotlin/JS browser bundle, copies it into the JVM resources, starts the local Ktor server, and
opens the application in the default browser. The default address is `http://127.0.0.1:8080/`.

Stop the application by closing its browser window and using the F95GM tray menu, or by stopping the Gradle process.

### Browser development mode

For JS hot reload, use two terminals. Start the JVM proxy on port 8081:

```powershell
.\gradlew.bat -Pf95gm.port=8081 runJvm
```

In a second terminal, start the Kotlin/JS development server:

```powershell
.\gradlew.bat jsBrowserDevelopmentRun
```

Open `http://127.0.0.1:8080/`. The development server proxies `/api/*` to the JVM process on port 8081. Running only the
JS development server is not enough for login or catalog requests; it serves browser assets but has no upstream proxy.

### Build artifacts

```powershell
.\gradlew.bat jsBrowserDistribution
.\gradlew.bat jvmJar
```

`jvmJar` includes the production browser bundle under the JVM `static/` resources. During development,
`-Df95gm.staticDir=<directory>` can point the JVM server at an external static directory.

### Build an installer

Use the platform's native installer task:

```powershell
.\gradlew.bat jpackage --no-configuration-cache
```

On Windows this creates `packaging/build/installer/f95gm-1.0.0.msi`. The packaging module selects MSI on Windows, DMG on
macOS, and DEB on Linux. The installed application starts its local server on an available loopback port and opens the
browser at the actual port.

## Local configuration and data

The JVM server listens on `127.0.0.1:8080` by default. These system properties can be supplied to `runJvm` or another
JVM launch command:

| Property          | Default           | Purpose                                               |
|-------------------|-------------------|-------------------------------------------------------|
| `f95gm.port`      | `8080`            | Local HTTP port; use `0` to select an available port. |
| `f95gm.host`      | `127.0.0.1`       | Bind address for the local server.                    |
| `f95gm.database`  | `~/F95GM/f95gm`   | H2 database file path, or a complete H2 JDBC URL.     |
| `f95gm.staticDir` | bundled resources | Optional directory containing the browser files.      |

For example:

```powershell
.\gradlew.bat -Df95gm.database="C:\data\f95gm" -Pf95gm.port=8082 runJvm
```

The default database location is `%USERPROFILE%\F95GM\f95gm` on Windows and `$HOME/F95GM/f95gm` on macOS/Linux. H2
creates the database files as needed. Back up this location while F95GM is stopped if you need a complete local data
backup; saved filters also have an in-app export/import option.

Do not put F95zone credentials in source files, `.env`, Gradle properties, or command history. Login credentials are
submitted through the UI and are used only to establish the upstream session.

## How it works internally

F95GM is a Kotlin Multiplatform project with two cooperating targets:

```text
Browser (Kotlin/JS + Fritz2)
        │  same-origin /api requests
        ▼
Local Ktor server (Kotlin/JVM)
        │  authenticated HTTP requests with upstream cookies
        ▼
F95zone latest-alpha pages, catalog API, threads, and media hosts
```

### Browser client

The Kotlin/JS client uses Fritz2 for reactive UI state and routing. It renders the catalog, filter controls,
saved-filter links, detail pages, tracked games, settings, login flow, loading states, and connection diagnostics.
Ktor's JS client calls only the local `/api` endpoints, so browser code does not need to manage F95zone cookies or
cross-origin requests.

The production bundle is generated by `jsBrowserDistribution`. In development, Kotlin's Webpack dev server forwards
`/api` to the JVM server at `127.0.0.1:8081`.

### Local JVM server

The Kotlin/JVM side runs an embedded Ktor CIO server. It:

1. Initializes the H2 database and restores non-expired sessions.
2. Serves the bundled SPA and its static assets.
3. Accepts login credentials, obtains the F95zone login token, follows the login redirect, and verifies the resulting
   authenticated session.
4. Stores the upstream cookie set in the local session record and associates it with the signed-in account.
5. Fetches the latest-alpha catalog through the current `latest_data.php` endpoint using the same request contract as
   the source page.
6. Follows canonical thread redirects, parses the authenticated first post, sanitizes the HTML, and returns structured
   detail data to the browser.
7. Rewrites supported F95zone attachment/preview image URLs to the session-protected local media endpoint, avoiding
   unauthenticated hotlink failures.
8. Persists sessions, game tracking states, and saved filters through Exposed tables backed by H2.

The server exposes local endpoints for session status/login/logout, catalog options and data, marks/tracked games,
saved-filter CRUD and backup/restore, item details, media, health checks, and desktop shutdown. Local routes such as
`/item/{threadId}` are mapped back to the SPA so refreshing a detail page still works.

### Persistence and update checks

Tracked games are keyed by the normalized signed-in F95zone account. When tracked games are loaded or checked, F95GM
fetches current thread metadata through the authenticated proxy and compares it with the stored version. A newer version
resets the relevant tracking state and produces the update indicator. The browser can also run checks at the configured
interval while the Tracked games page is available.

### Desktop lifecycle

The JVM entry point starts the server, creates a desktop tray controller, and opens the browser. The packaged
application uses a randomly assigned loopback port. A browser heartbeat lets the desktop process detect when its
application tabs are gone and shut the local server down cleanly.

## Project layout

- `src/commonMain`: shared domain models, API contracts, and generated localization support.
- `src/jsMain`: Fritz2 browser UI, client state/actions, routing, and browser resources.
- `src/jvmMain`: Ktor server, upstream requests/parsing, media proxy, H2 persistence, logging, and desktop lifecycle.
- `packaging`: Beryx Runtime and `jpackage` configuration for native installers.
- `webpack.config.d`: Kotlin/JS Webpack configuration, including the development API proxy.

The upstream endpoint currently used by the application is the alpha catalog endpoint. The former `new_latest.php`
endpoint is intentionally not used because it is disabled upstream and returns HTTP 403.
