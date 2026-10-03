$ErrorActionPreference = 'Stop'

# Builds a Windows installer whose runtime configuration contains only the public API URL.
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Maven build failed; installer was not created." }
mvn dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target\package-input\lib
if ($LASTEXITCODE -ne 0) { throw "Dependency packaging failed; installer was not created." }
New-Item -ItemType Directory -Force target\package-input | Out-Null
Copy-Item target\SyncSphere-1.0.0.jar target\package-input\
New-Item -ItemType Directory -Force dist | Out-Null

jpackage `
  --type exe `
  --name SyncSphere `
  --input target\package-input `
  --main-jar SyncSphere-1.0.0.jar `
  --main-class com.syncsphere.Main `
  --dest dist `
  --app-version 1.0.0 `
  --vendor SyncSphere `
  --java-options "-DSYNCSPHERE_PACKAGED=true" `
  --win-dir-chooser `
  --win-shortcut

if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed. Install WiX Toolset and ensure candle.exe and light.exe are on PATH."
}

Write-Host "Created dist\SyncSphere-1.0.0.exe"
Write-Host "Set SYNCSPHERE_API_URL on the client before launching."
