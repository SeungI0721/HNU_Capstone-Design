# Smart Shield HW

Smart Shield 하드웨어 펌웨어와 테스트 코드를 관리하는 폴더입니다. ESP32 본 펌웨어, 센서 테스트, 출력 모듈 테스트, 통합 테스트 코드가 포함됩니다.

## 폴더 구조

```text
HW/
  SmartShield_ESP32/
  TestCode/
```

## 하위 폴더 요약

| 폴더 | 내용 |
| --- | --- |
| `SmartShield_ESP32` | 실제 시제품에 업로드하는 ESP32 본 펌웨어입니다. 센서 측정, BLE Notify 전송, BLE Write 명령 수신, RGB LED, 진동 모터, 부저 제어를 담당합니다. |
| `TestCode` | 센서와 출력 모듈을 개별 또는 통합으로 검증하는 테스트 스케치 모음입니다. |

## 전체 하드웨어 기준

| 항목 | 기준 |
| --- | --- |
| 보드 | ESP32 Dev Module |
| 개발 환경 | Arduino IDE 또는 Arduino CLI |
| FQBN | `esp32:esp32:esp32` |
| 시리얼 속도 | `115200` baud |
| I2C SDA | GPIO21 |
| I2C SCL | GPIO22 |
| BLE 역할 | Peripheral / GATT Server |
| Android 연동 | 작업자 앱 `HNU_PPE_Control` |

## 문서 기준

상위 README는 하위 폴더 구조와 역할만 요약합니다. 실제 파일별 상세 내용은 각 하위 폴더 README에 작성합니다.
