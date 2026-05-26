# SmartShield_ESP32

ESP32 최종 통합 펌웨어입니다. 센서 데이터를 읽어 BLE Notify payload로 작업자 앱에 전송하고, 작업자 앱에서 받은 `RISK:*` 명령에 따라 RED LED, 진동모터, 부저를 제어합니다.

## 주요 파일

| 파일 | 역할 |
|---|---|
| `SmartShield_ESP32.ino` | 전역 설정, 핀, UUID, setup/loop |
| `SmartShield_Sensors.h` | I2C 센서 초기화와 센서값 읽기 |
| `SmartShield_Payload.h` | BLE Notify payload 생성 |
| `SmartShield_Ble.h` | BLE GATT Server, Notify, Write 처리 |
| `SmartShield_Outputs.h` | RED LED, 진동모터, 부저 출력 제어 |
| `SmartShield_Utils.h` | 공통 유틸리티 |

## BLE 규격

| 항목 | 값 |
|---|---|
| Device Name | `SS_0001` |
| Service UUID | `089fca17-755f-4578-b8af-ee5e32526b0f` |
| Notify UUID | `0000FFF1-0000-1000-8000-00805F9B34FB` |
| Write UUID | `0000FFF2-0000-1000-8000-00805F9B34FB` |
| CCCD UUID | `00002902-0000-1000-8000-00805F9B34FB` |

## Notify payload 예시

```text
ID:0001,TEMP:34.8,TEMP_VALID:1,TEMP_SOURCE:MEASURED,HR:82,SPO2:98,ENV:28.4,HUM:55,LUX:1200,AX:0.01,AY:0.02,AZ:9.80,POSTURE:NORMAL
```

TEMP 센서 읽기 실패 시 정상값처럼 보이는 fallback을 보내지 않고 다음처럼 전송합니다.

```text
TEMP:0.0,TEMP_VALID:0,TEMP_SOURCE:INVALID
```

## Write 명령

```text
RISK:SAFE
RISK:CAUTION
RISK:DANGER
RISK:EMERGENCY
```

## 핀과 센서 주소

| 항목 | 코드 기준 |
|---|---|
| I2C SDA | GPIO21 |
| I2C SCL | GPIO22 |
| RED LED | `LED_R_PIN` |
| 진동모터 | `VIBRATION_PIN` |
| 부저 | `BUZZER_PIN` |
| MAX30205 | `0x48` |
| MAX30102 | `0x57` |

## 빌드

```powershell
arduino-cli compile --fqbn esp32:esp32:esp32 "d:\HNU\HW\SmartShield_ESP32"
```

## 실기 확인

- Serial Monitor에서 I2C 센서 감지 로그 확인
- MAX30205 `TEMP_VALID` 1/0 전환 확인
- BLE payload 길이와 Notify 수신 여부 확인
- 앱에서 `RISK:*` 명령을 보냈을 때 RED LED, 진동모터, 부저 동작 확인
