# Module Test Code

출력 모듈 단독 테스트 폴더입니다. LED, 진동 모터, 부저가 ESP32 핀 제어에 맞게 동작하는지 확인합니다.

## 파일 구성

| 파일 | 내용 |
|---|---|
| `YwRobot_RGB_LED/YwRobot_RGB_LED.ino` | YwRobot RGB LED 색상 출력 테스트 |
| `Vibration_Motor/Vibration_Motor.ino` | 진동 모터 ON/OFF 테스트 |
| `Vibration_Motor_PWM/Vibration_Motor_PWM.ino` | PWM을 이용한 진동 모터 세기 제어 테스트 |
| `Passive_Buzzer/Passive_Buzzer.ino` | 패시브 부저 주파수 출력 테스트 |

## 코드 흐름

```text
테스트할 출력 모듈 연결
→ 해당 하위 폴더의 스케치 컴파일
→ ESP32 업로드
→ 출력 패턴을 눈, 진동, 소리로 확인
```

## 실행 방법

각 모듈 폴더에서 Arduino CLI를 실행합니다.

```powershell
arduino-cli compile --fqbn esp32:esp32:esp32 .
arduino-cli upload --fqbn esp32:esp32:esp32 -p COM8 .
arduino-cli monitor -p COM8 -c baudrate=115200
```

## 테스트 핀

| 모듈 | ESP32 핀 |
|---|---|
| RGB LED R | GPIO27 |
| RGB LED G | GPIO32 |
| RGB LED B | GPIO33 |
| 진동 모터 | GPIO23 |
| 부저 | GPIO18 |

최종 `SmartShield_ESP32` 펌웨어는 RGB LED 전체가 아니라 GPIO27 단일 RED LED만 사용합니다.

## 주의 사항

- 진동 모터는 ESP32 GPIO에 직접 연결하지 말고 MOSFET 또는 트랜지스터와 플라이백 다이오드를 사용합니다.
- RGB LED 모듈은 공통 애노드인지 공통 캐소드인지 확인합니다.
- 패시브 부저는 PWM 주파수로 제어합니다.
- 액티브 부저를 사용할 경우 단순 ON/OFF 방식으로 동작할 수 있습니다.

## 외부 의존성

| 항목 | 설명 |
|---|---|
| Arduino CLI | 스케치 빌드와 업로드 |
| ESP32 board package | `esp32:esp32:esp32` |
| 구동 회로 | 진동 모터와 부저는 트랜지스터 또는 MOSFET 구동 권장 |

## 테스트 방법

출력 모듈이 지정된 핀에서 의도한 패턴으로 동작하는지 확인합니다. 최종 펌웨어는 위험도별 출력 패턴을 별도로 정의하므로, 단독 테스트 통과 후 `Warning_Integration_Test_Code`와 `SmartShield_ESP32`에서 다시 확인합니다.

## 관련 문서

| 문서 | 설명 |
|---|---|
| [TestCode README](../README.md) | 하드웨어 테스트 순서 |
| [상위 HW README](../../README.md) | 최종 출력 장치 기준 |
| [최종 펌웨어 README](../../SmartShield_ESP32/README.md) | 실제 위험도 출력 패턴 |
