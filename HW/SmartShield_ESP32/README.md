# SmartShield_ESP32

ESP32 최종 통합 펌웨어입니다. I2C 센서 데이터를 읽어 BLE Notify payload로 작업자 Android 앱에 전송하고, 앱에서 받은 `RISK:*` 명령에 따라 RED LED, 진동 모터, 부저를 제어합니다.

## 주요 파일

| 파일 | 역할 |
|---|---|
| `SmartShield_ESP32.ino` | 전역 설정, 핀, UUID, 상태 변수, `setup()`, `loop()` |
| `SmartShield_Sensors.h` | I2C 스캔, 센서 초기화, 센서 값 읽기 |
| `SmartShield_Payload.h` | BLE Notify payload 생성 및 전송 |
| `SmartShield_Ble.h` | BLE GATT Server, Notify, Write 명령 처리 |
| `SmartShield_Outputs.h` | RED LED, 진동 모터, 부저 출력 패턴 제어 |
| `SmartShield_Utils.h` | 범위 검사, 포맷팅, fallback 유틸리티 |
| `compile.ps1` | Arduino CLI 컴파일 스크립트 |
| `upload.ps1` | ESP32 업로드 스크립트 |
| `monitor.ps1` | 시리얼 모니터 실행 스크립트 |

## 동작 흐름

```text
setup()
  Serial 시작
  출력 핀 초기화
  I2C 센서 스캔 및 초기화
  BLE GATT Server 시작

loop()
  연결 해제 후 advertising 재시작 처리
  위험도에 따른 LED blink 갱신
  진동/부저 패턴 갱신
  MAX30102 샘플 갱신
  1초마다 Notify payload 생성 및 전송
```

## 주요 설정

| 항목 | 값 |
|---|---|
| Worker ID | `0001` |
| BLE Device Name | `SS_0001` |
| Service UUID | `089fca17-755f-4578-b8af-ee5e32526b0f` |
| Notify UUID | `0000FFF1-0000-1000-8000-00805F9B34FB` |
| Write UUID | `0000FFF2-0000-1000-8000-00805F9B34FB` |
| CCCD UUID | `00002902-0000-1000-8000-00805F9B34FB` |
| Notify interval | `1000 ms` |
| BLE MTU | `128` |
| Fake data mode | `false` |
| Packet markers | `false` |

## 핀과 I2C 주소

| 항목 | 코드 기준 |
|---|---|
| I2C SDA | GPIO21 |
| I2C SCL | GPIO22 |
| RED LED | GPIO27 |
| 진동 모터 | GPIO23 |
| 부저 | GPIO18 |
| BME280 | `0x76` 또는 `0x77` |
| MPU6050 | `0x68` 또는 `0x69` |
| BH1750 | `0x23` 또는 `0x5C` |
| MAX30102 | `0x57` |
| MAX30205 | `0x48` |

## Notify payload

현재 기본 payload 예시는 다음과 같습니다.

```text
ID:0001,TEMP:34.8,TEMP_VALID:1,TEMP_SOURCE:MEASURED,HR:82,SPO2:98,ENV:28.4,HUM:55,LUX:1200,AX:0.01,AY:0.02,AZ:9.80,POSTURE:NORMAL
```

센서별 처리 기준은 다음과 같습니다.

| 필드 | 출처 | 실패 시 처리 |
|---|---|---|
| `TEMP` | MAX30205 | `0.0`, `TEMP_VALID:0`, `TEMP_SOURCE:INVALID` |
| `HR` | MAX30102 | fallback `82` |
| `SPO2` | MAX30102 | fallback `98` |
| `ENV` | BME280 | fallback `28.5` |
| `HUM` | BME280 | fallback `55` |
| `LUX` | BH1750 | fallback `8000` |
| `AX`, `AY`, `AZ` | MPU6050 | fallback `0.00` |
| `POSTURE` | MPU6050 기반 계산 | fallback `NORMAL` |

`POSTURE` 값은 `NORMAL`, `WARNING`, `UNSTABLE`, `FALL` 중 하나로 생성됩니다. Android 파서는 추가로 `EMERGENCY`도 허용합니다.

## Write 명령

작업자 앱은 위험도 계산 결과를 다음 명령으로 보냅니다.

```text
RISK:SAFE
RISK:CAUTION
RISK:DANGER
RISK:EMERGENCY
```

펌웨어는 `RISK:ERROR`를 받으면 `CAUTION` 출력으로 처리합니다.

## 출력 패턴

| 위험도 | RED LED | 진동 | 부저 |
|---|---|---|---|
| `SAFE` | OFF | OFF | OFF |
| `CAUTION` | 500ms blink | 1회 200ms | 1회 1000Hz 200ms |
| `DANGER` | ON | 반복 300ms | 반복 2000Hz 300ms |
| `EMERGENCY` | ON | 반복 500ms | 반복 3000Hz 150ms |

## 빌드와 업로드

```powershell
.\compile.ps1
.\upload.ps1 -Port COM8
.\monitor.ps1 -Port COM8
```

직접 Arduino CLI를 사용할 수도 있습니다.

```powershell
arduino-cli compile --fqbn esp32:esp32:esp32 HW\SmartShield_ESP32
arduino-cli upload -p COM8 --fqbn esp32:esp32:esp32 HW\SmartShield_ESP32
arduino-cli monitor -p COM8 -c baudrate=115200
```

보드가 연결되어 있지 않으면 COM 포트가 보이지 않거나 업로드가 실패하는 것이 정상입니다.

## 시리얼 확인 항목

- 부팅 메시지와 `Fake Data Test Mode`, `App Safe Fallback Values` 상태
- I2C Scanner에서 감지되는 센서 주소
- 각 센서의 `OK` 또는 `NOT FOUND` 로그
- MAX30205 `TEMP_VALID` 1/0 전환
- MAX30102 IR/RED, finger, fallback 여부
- `[NOTIFY]` payload 길이와 필드 구성
- 앱 연결 후 `[BLE NOTIFY] sent`
- 앱에서 보낸 `RISK:*` 명령 수신과 출력 동작
