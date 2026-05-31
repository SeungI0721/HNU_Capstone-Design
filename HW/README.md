# Smart Shield HW

이 폴더는 ESP32 펌웨어와 하드웨어 검증용 테스트 코드를 포함합니다.

## 폴더 구조

| 경로 | 설명 |
|---|---|
| `SmartShield_ESP32/` | 최종 통합 ESP32 펌웨어 |
| `TestCode/` | 센서, 출력 모듈, 통합 테스트용 Arduino 스케치 |

## 최종 하드웨어 기준

| 구분 | 장치 | 통신/제어 | 코드 기준 |
|---|---|---|---|
| 메인 보드 | ESP32 Dev Module | Arduino / BLE | `esp32:esp32:esp32` |
| 환경 센서 | BME280 | I2C | `0x76` 또는 `0x77` |
| 조도 센서 | BH1750 / GY-302 | I2C | `0x23` 또는 `0x5C` |
| 자세 센서 | MPU6050 / GY-521 | I2C | `0x68` 또는 `0x69` |
| 심박/SpO2 보조 | MAX30102 | I2C | `0x57` |
| 피부 온도 | MAX30205 / Fever Click | I2C | `0x48` |
| 시각 경고 | 단일 RED LED | GPIO | GPIO27 |
| 촉각 경고 | 진동 모터 | GPIO | GPIO23 |
| 청각 경고 | 부저 | PWM | GPIO18 |

## 공통 배선

| 기능 | ESP32 핀 |
|---|---|
| I2C SDA | GPIO21 |
| I2C SCL | GPIO22 |
| RED LED | GPIO27 |
| 진동 모터 | GPIO23 |
| 부저 | GPIO18 |

진동 모터는 ESP32 GPIO에 직접 연결하지 말고 MOSFET 또는 트랜지스터와 플라이백 다이오드를 사용해 구동하는 것을 권장합니다.

## 주의 사항

- 최종 펌웨어는 RGB LED가 아니라 단일 RED LED만 사용합니다.
- MAX30205 주소는 현재 코드에서 `0x48`만 확인합니다. 다른 보드나 배선에서는 I2C Scanner로 실제 주소를 먼저 확인합니다.
- MAX30102 HR/SpO2 값은 간이 추정값입니다. 착용 상태, 손가락 접촉, 움직임에 크게 영향을 받으므로 실측 안정성을 별도로 확인해야 합니다.
- 센서가 없거나 읽기 실패한 경우 일부 값은 fallback으로 채워질 수 있습니다. 피부 온도는 `TEMP_VALID:0`, `TEMP_SOURCE:INVALID`로 구분합니다.
- 이 시스템은 산업안전 보조 장치이며 의료 진단 장치가 아닙니다.

## 빌드

```powershell
arduino-cli compile --fqbn esp32:esp32:esp32 HW\SmartShield_ESP32
```

업로드는 실제 ESP32 보드가 연결되어 있고 COM 포트가 인식된 뒤 실행합니다.

```powershell
arduino-cli upload -p COM8 --fqbn esp32:esp32:esp32 HW\SmartShield_ESP32
```

## 실측 검증

하드웨어 실측 결과는 `docs/MEASUREMENT_VERIFICATION.md` 기준으로 기록합니다.
