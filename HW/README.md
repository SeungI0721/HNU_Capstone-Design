# Smart Shield HW

이 폴더는 ESP32 펌웨어와 센서/모듈 테스트 코드를 포함합니다.

## 폴더 구조

| 경로 | 설명 |
|---|---|
| `SmartShield_ESP32/` | 최종 통합 ESP32 펌웨어 |
| `TestCode/` | 센서, 출력 모듈, 통합 테스트용 스케치 |

## 최종 하드웨어 기준

| 구분 | 장치 | 통신/제어 | 코드 기준 |
|---|---|---|---|
| 메인 보드 | ESP32 DevKit | Arduino / BLE | 최종 보드 |
| 환경 센서 | BME280 | I2C | `0x76` 또는 `0x77` |
| 조도 센서 | BH1750 / GY-302 | I2C | `0x23` 또는 `0x5C` |
| 자세 센서 | MPU6050 / GY-521 | I2C | `0x68` 또는 `0x69` |
| 심박/SpO2 보조 | MAX30102 / SEN0344 | I2C | `0x57` |
| 피부 접촉 온도 | MAX30205 / Fever Click | I2C | 현재 코드 `0x48` |
| 시각 경고 | RED LED | GPIO | RGB가 아닌 단일 RED LED |
| 촉각 경고 | 진동모터 | GPIO | 트랜지스터 또는 MOSFET 구동 권장 |
| 청각 경고 | 부저 | PWM | 위험 단계별 패턴 |

## 주의 사항

- 현재 최종 코드는 RGB LED가 아니라 단일 RED LED를 사용합니다.
- MAX30205 주소는 코드상 `0x48`이지만, 실물 모듈은 배선/보드에 따라 다를 수 있으므로 I2C Scanner로 반드시 확인해야 합니다.
- MAX30102의 HR/SpO2 값은 움직임, 착용 압박, 외부광 영향을 크게 받으므로 실측값과 fallback 구분을 따로 검증해야 합니다.
- 이 시스템은 산업안전 보조 장치이며 의료 진단 장치가 아닙니다.

## 빌드

```powershell
arduino-cli compile --fqbn esp32:esp32:esp32 "d:\HNU\HW\SmartShield_ESP32"
```

## 실측 검증

하드웨어 실측 항목은 `docs/MEASUREMENT_VERIFICATION.md`를 기준으로 기록합니다.
