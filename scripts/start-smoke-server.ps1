param(
    [string]$ServerDir = ".smoke-server",
    [string]$PaperVersion = "latest",
    [switch]$SkipBuild,
    [switch]$Help
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($Help) {
    Write-Host "Builds MobRarity, downloads Paper, installs the plugin, and starts a local smoke-test server."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  ./scripts/start-smoke-server.ps1"
    Write-Host "  ./scripts/start-smoke-server.ps1 -PaperVersion 1.21.10"
    Write-Host "  ./scripts/start-smoke-server.ps1 -ServerDir C:\Temp\mobrarity-server -SkipBuild"
    exit 0
}

function Resolve-RepoRoot {
    $root = git rev-parse --show-toplevel
    if (-not $root) {
        throw "This script must be run inside the havenmobs git checkout."
    }
    return $root.Trim()
}

function Get-LatestPaperVersion {
    $project = Invoke-RestMethod -Uri "https://api.papermc.io/v2/projects/paper"
    return $project.versions[-1]
}

function Get-LatestPaperBuild([string]$Version) {
    $versionInfo = Invoke-RestMethod -Uri "https://api.papermc.io/v2/projects/paper/versions/$Version"
    return $versionInfo.builds[-1]
}

function Download-PaperServer([string]$Version, [int]$Build, [string]$Destination) {
    $fileName = "paper-$Version-$Build.jar"
    $downloadUrl = "https://api.papermc.io/v2/projects/paper/versions/$Version/builds/$Build/downloads/$fileName"
    if (-not (Test-Path $Destination)) {
        Write-Host "Downloading $fileName"
        Invoke-WebRequest -Uri $downloadUrl -OutFile $Destination
    } else {
        Write-Host "Using cached Paper jar: $Destination"
    }
}

$repoRoot = Resolve-RepoRoot
$serverPath = if ([System.IO.Path]::IsPathRooted($ServerDir)) {
    $ServerDir
} else {
    Join-Path $repoRoot $ServerDir
}

if (-not $SkipBuild) {
    Push-Location $repoRoot
    try {
        ./gradlew.bat build
    } finally {
        Pop-Location
    }
}

$pluginJar = Join-Path $repoRoot "build/libs/MobRarity-1.0.0-SNAPSHOT.jar"
if (-not (Test-Path $pluginJar)) {
    throw "Missing plugin jar: $pluginJar. Run without -SkipBuild or build the project first."
}

if ($PaperVersion -eq "latest") {
    $PaperVersion = Get-LatestPaperVersion
}
$paperBuild = Get-LatestPaperBuild $PaperVersion

New-Item -ItemType Directory -Path $serverPath -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $serverPath "plugins") -Force | Out-Null

$paperJar = Join-Path $serverPath "paper-$PaperVersion-$paperBuild.jar"
Download-PaperServer $PaperVersion $paperBuild $paperJar

Copy-Item -LiteralPath $pluginJar -Destination (Join-Path $serverPath "plugins/MobRarity.jar") -Force
Set-Content -Path (Join-Path $serverPath "eula.txt") -Value "eula=true"

$serverProperties = Join-Path $serverPath "server.properties"
if (-not (Test-Path $serverProperties)) {
    @"
online-mode=false
level-name=mobrarity-smoke
gamemode=creative
difficulty=normal
spawn-protection=0
enable-command-block=true
"@ | Set-Content -Path $serverProperties
}

Write-Host "Starting Paper $PaperVersion build $paperBuild"
Write-Host "Server directory: $serverPath"
Write-Host "Installed plugin: plugins/MobRarity.jar"
Write-Host "Stop the server with the 'stop' console command."

Push-Location $serverPath
try {
    java -Xms1G -Xmx2G -jar $paperJar nogui
} finally {
    Pop-Location
}
