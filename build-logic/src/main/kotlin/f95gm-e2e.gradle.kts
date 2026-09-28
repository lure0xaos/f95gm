import f95gm.build.BrowserE2eTask

tasks.register<BrowserE2eTask>("browserE2e") {
    group = "verification"
    description = "Runs the Playwright browser smoke test against a packaged JVM server."
    notCompatibleWithConfigurationCache("The smoke task owns external server and browser processes")
    dependsOn(":packaging:installDist")
    launcher.set(layout.projectDirectory.file("packaging/build/install/packaging/bin/packaging.bat"))
    e2eDirectory.set(layout.projectDirectory.dir("e2e"))
    databaseDirectory.set(layout.buildDirectory.dir("browser-e2e-data"))
    port.convention(18_080)
    upstreamPort.convention(18_081)
    timeoutSeconds.convention(300)
    npmExecutable.convention(
        providers.systemProperty("os.name").map { osName ->
            if (osName.contains("Windows", ignoreCase = true)) "npx.cmd" else "npx"
        }
    )
}
