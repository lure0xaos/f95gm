param(
    [Parameter(Mandatory = $true)]
    [string] $JavaExe
)

$versionOutput = (& $JavaExe -version 2>&1 | Out-String)
if ($versionOutput -notmatch 'version "(?<version>[^"]+)"') {
    Write-Error "Unable to determine the Java version from $JavaExe."
    exit 1
}

$version = $Matches.version
$major = if ($version.StartsWith('1.')) {
    [int]$version.Substring(2).Split('.')[0]
} else {
    [int]$version.Split('.')[0]
}

if ($major -lt 17) {
    Write-Error "F95GM requires Java 17 or newer; detected Java $major. Set JAVA_HOME to a JDK 17+ installation."
    exit 1
}
