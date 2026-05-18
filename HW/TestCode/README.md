# Smart Shield TestCode

Smart Shield 하드웨어를 단계별로 검증하기 위한 테스트 코드 폴더입니다. 센서 단독 테스트, 출력 모듈 단독 테스트, 통합 테스트, 경고 출력 테스트가 포함됩니다.

## 폴더 구조

```text
TestCode/
  Sensor/
  Module/
  Integration_Test_Code/
  Warning_Integration_Test_Code/
```

## 하위 폴더 요약

| 폴더 | 내용 |
| --- | --- |
| `Sensor` | BME280, BH1750, MPU6050, MAX30102, MAX30205, I2C Scanner 테스트 스케치가 있습니다. |
| `Module` | RGB LED, 진동 모터, PWM 진동 모터, 패시브 부저 테스트 스케치가 있습니다. |
| `Integration_Test_Code` | 여러 센서를 한 번에 초기화하고 시리얼 출력으로 확인하는 통합 테스트입니다. |
| `Warning_Integration_Test_Code` | 위험 단계별 LED, 진동, 부저 출력 패턴을 확인하는 통합 테스트입니다. |

## 테스트 순서

```text
1. Sensor/I2C_Seaner로 I2C 주소 확인
2. Sensor 폴더의 센서 단독 테스트 실행
3. Module 폴더의 출력 모듈 단독 테스트 실행
4. Integration_Test_Code로 센서 통합 테스트 실행
5. Warning_Integration_Test_Code로 경고 출력 통합 테스트 실행
6. SmartShield_ESP32 본 펌웨어 업로드
```

## 공통 기준

| 항목 | 값 |
| --- | --- |
| 보드 | ESP32 Dev Module |
| FQBN | `esp32:esp32:esp32` |
| 시리얼 속도 | `115200` baud |
| I2C SDA | GPIO21 |
| I2C SCL | GPIO22 |

각 하위 폴더의 구체적인 파일 역할과 실행 방법은 해당 폴더 README를 확인합니다.
