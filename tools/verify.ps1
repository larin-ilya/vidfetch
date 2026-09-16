# One-command verification of the VidFetch download engine.
# Usage:  pwsh -File tools/verify.ps1
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$env:VIDFETCH_FFMPEG_DIR = Join-Path $root "build\ffmpeg"

Write-Host "== 1) engine ping (expect ytdlp nightly) =="
'{"id":1,"method":"ping","params":{}}' | python (Join-Path $root "engine\engine.py")

Write-Host "== 2) real download smoke test =="
python (Join-Path $root "tests\smoke_test.py")

Write-Host "== 3) frozen engine ping (if built) =="
$exe = Join-Path $root "engine\dist\engine.exe"
if (Test-Path $exe) {
  '{"id":1,"method":"ping","params":{}}' | & $exe
} else {
  Write-Host "(engine.exe not built yet; skip)"
}
Write-Host "VERIFY DONE"
