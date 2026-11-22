$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location (Join-Path $root 'go-mobilecore')
if (-not (Get-Command go -ErrorAction SilentlyContinue)) { throw 'Cần cài Go' }
if (-not (Get-Command gomobile -ErrorAction SilentlyContinue)) {
  throw 'Cần chạy: go install golang.org/x/mobile/cmd/gomobile@latest ; thêm GOPATH\bin vào PATH'
}
if (-not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT) { throw 'Cần cấu hình ANDROID_HOME' }
go test ./...
if ($LASTEXITCODE -ne 0) { throw 'Go test failed' }
go list -m golang.org/x/mobile *> $null
if ($LASTEXITCODE -ne 0) {
  go get -tool golang.org/x/mobile/cmd/gobind
  if ($LASTEXITCODE -ne 0) { throw 'Không thể cài x/mobile vào go.mod' }
}
gomobile init
if ($LASTEXITCODE -ne 0) { throw 'gomobile init failed' }
$output = Join-Path $root 'android\app\libs\mobilecore.aar'
gomobile bind -target=android/arm64 -androidapi=26 -o $output .
if ($LASTEXITCODE -ne 0) { throw 'gomobile bind failed' }
Write-Host "Tạo xong $output"
