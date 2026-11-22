$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
foreach ($cmd in @('gradle','java','javac')) {
  if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) { throw "Thiếu công cụ: $cmd" }
}
if (-not ($env:ANDROID_HOME -or $env:ANDROID_SDK_ROOT)) { throw 'Thiếu ANDROID_HOME / ANDROID_SDK_ROOT' }
Set-Location (Join-Path $root 'android')
if (-not (Test-Path '.\gradle\wrapper\gradle-wrapper.jar')) {
  gradle wrapper --gradle-version 8.9
  if ($LASTEXITCODE -ne 0) { throw 'Gradle wrapper failed' }
}
.\gradlew.bat --no-daemon --stacktrace assembleDebug
if ($LASTEXITCODE -ne 0) { throw 'APK build failed' }
Write-Host "APK: $(Join-Path $root 'android\app\build\outputs\apk\debug\app-debug.apk')"
