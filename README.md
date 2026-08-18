# F95GM

F95GM is a Fritz2 catalog client for the authenticated F95zone latest alpha feed. It uses the same request contract as
the source page:

```text
GET https://f95zone.to/sam/latest_alpha/latest_data.php?cmd=list&cat=games&page=1&sort=date&rows=90&_=<timestamp>
```

The browser talks to the local JVM proxy instead of calling F95zone directly. The proxy performs the XenForo login,
stores upstream session cookies in the local H2 database, and forwards catalog requests.
Credentials are entered in the UI and are not stored in source, local storage, or `.env`.

Catalog cards navigate to local `/item/{threadId}` routes. The JVM Ktor proxy follows the thread's canonical redirect,
extracts the authenticated first post, and the app renders the title, author, cover, and detail content inside F95GM.

Thread images are delivered through the session-protected JVM `/api/media` proxy so F95zone attachment hotlink
restrictions do not leave blank images in the app.

## Run

Start the complete application with one task; `jvmRunApp` builds the JS distribution, copies it into JVM resources, and
starts the Ktor server:

```powershell
.\gradlew.bat jvmRunApp
```

`runJvm` remains available as the underlying JVM executable task.

For browser hot reload, use two terminals. Start the proxy on port 8081:

```powershell
.\gradlew.bat -Pf95gm.port=8081 runJvm
```

Then start the JS development server and open `http://127.0.0.1:8080/`:

```powershell
.\gradlew.bat jsBrowserDevelopmentRun
```

The development server proxies `/api/*` to the JVM proxy. Running the JS development server by itself will return 404
for login because it only serves browser assets.

For a packaged build, use `jvmJar`; the copied browser bundle is included under `static/`. The optional
`-Df95gm.staticDir` JVM property can override those classpath resources during development.

## Windows installer

Build the Beryx Runtime MSI from Windows with:

```powershell
.\gradlew.bat jpackage --no-configuration-cache
```

The installer is written to `packaging/build/installer/f95gm-1.0.0.msi`. Launching the installed app starts the local
server on an available loopback port and opens the browser at the server's actual address.

The proxy uses the current alpha data endpoint. The former `new_latest.php` endpoint is disabled upstream and returns
403; F95GM no longer calls that former endpoint.

Game tracking states such as Watching, Downloaded, Playable, Paused, and Completed are stored locally by the JVM proxy
in the H2 database, keyed by the signed-in F95zone account. They are never sent to F95zone. The My games page checks
marked threads for newer versions; an update remains highlighted until the stored version is updated with the downloaded
action.

Named catalog filter presets are stored in the same H2 database, also keyed by the signed-in account. Save a
preset from the filter panel, use its quick link beside My games, or remove it from the saved-links list; these presets
never reach F95zone.

The local session record is restored after a JVM restart and expires after the same 30-day lifetime as the browser
cookie. If the upstream F95zone session itself expires or is rejected, F95GM clears the local session and requires login
again.
