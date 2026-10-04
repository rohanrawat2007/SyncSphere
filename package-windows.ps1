$ErrorActionPreference = 'Stop'

# Builds a portable Windows app image. Its SyncSphere.exe launches the application directly.
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot
$maven = 'C:\tools\apache-maven-3.9.9\bin\mvn.cmd'
$jpackage = 'C:\Program Files\Java\jdk-21.0.11\bin\jpackage.exe'
if (-not (Test-Path $jpackage)) { throw "jpackage.exe was not found at $jpackage" }

& $maven clean package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Maven build failed; installer was not created." }
& $maven dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target\package-input\lib
if ($LASTEXITCODE -ne 0) { throw "Dependency packaging failed; installer was not created." }
New-Item -ItemType Directory -Force target\package-input | Out-Null
Copy-Item target\SyncSphere-1.0.0.jar target\package-input\
New-Item -ItemType Directory -Force dist | Out-Null

$portableDestination = Join-Path $projectRoot 'dist\portable'
if (Test-Path $portableDestination) { Remove-Item -LiteralPath $portableDestination -Recurse -Force }

& $jpackage `
  --type app-image `
  --name SyncSphere `
  --input target\package-input `
  --main-jar SyncSphere-1.0.0.jar `
  --main-class com.syncsphere.Main `
  --dest $portableDestination `
  --app-version 1.0.0 `
  --vendor SyncSphere `
  --java-options "-DSYNCSPHERE_PACKAGED=true"

if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed. Ensure a JDK with jpackage is installed and available on PATH."
}

Write-Host "Created dist\portable\SyncSphere\SyncSphere.exe"
Write-Host "This executable launches SyncSphere directly; no installer step is required."

$installerDestination = Join-Path $projectRoot 'dist\installer'
if (Test-Path $installerDestination) { Remove-Item -LiteralPath $installerDestination -Recurse -Force }
& $jpackage `
  --type exe `
  --name SyncSphere `
  --input target\package-input `
  --main-jar SyncSphere-1.0.0.jar `
  --main-class com.syncsphere.Main `
  --dest $installerDestination `
  --app-version 1.0.0 `
  --vendor SyncSphere `
  --java-options "-DSYNCSPHERE_PACKAGED=true" `
  --win-dir-chooser `
  --win-menu `
  --win-shortcut
if ($LASTEXITCODE -ne 0) { throw "jpackage could not create the Windows installer." }

$installer = Get-ChildItem -Path $installerDestination -Filter '*.exe' | Select-Object -First 1
Copy-Item -LiteralPath $installer.FullName -Destination (Join-Path $projectRoot 'dist\SyncSphere-Setup.exe') -Force
Write-Host "Created shareable installer dist\SyncSphere-Setup.exe"
