# 통합 테스트 스케치를 Arduino CLI로 컴파일하는 스크립트
param(
  [string]$Fqbn = "esp32:esp32:esp32"
)

$ErrorActionPreference = "Stop"

$Cli = "C:\Program Files\Arduino IDE\resources\app\lib\backend\resources\arduino-cli.exe"
if (-not (Test-Path $Cli)) {
  $Cli = (Get-Command arduino-cli -ErrorAction Stop).Source
}

& $Cli compile --fqbn $Fqbn $PSScriptRoot
