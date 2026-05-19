param(
  [string]$Port = "",
  [string]$Fqbn = "esp32:esp32:esp32"
)

$ErrorActionPreference = "Stop"

$Cli = "C:\Program Files\Arduino IDE\resources\app\lib\backend\resources\arduino-cli.exe"
if (-not (Test-Path $Cli)) {
  $Cli = (Get-Command arduino-cli -ErrorAction Stop).Source
}

if ([string]::IsNullOrWhiteSpace($Port)) {
  $boardList = & $Cli board list
  $ports = @(
    $boardList |
      Select-String -Pattern "COM\d+" -AllMatches |
      ForEach-Object { $_.Matches.Value } |
      Select-Object -Unique
  )

  if ($ports.Count -eq 1) {
    $Port = $ports[0]
  } elseif ($ports.Count -gt 1) {
    Write-Host "Multiple serial ports found:"
    $ports | ForEach-Object { Write-Host "  $_" }
    throw "Run .\upload.ps1 -Port COMx with the ESP32 USB serial port."
  } else {
    throw "No serial port found. Connect the ESP32 and run again, or use .\upload.ps1 -Port COMx."
  }
}

& $Cli upload -p $Port --fqbn $Fqbn $PSScriptRoot
