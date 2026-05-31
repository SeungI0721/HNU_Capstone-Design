# Module Test Code

출력 모듈 단독 테스트 폴더입니다. LED, 진동 모터, 부저가 ESP32 핀 제어에 맞게 동작하는지 확인합니다.

## 파일 구성

| 파일 | 내용 |
|---|---|
| `YwRobot_RGB_LED/YwRobot_RGB_LED.ino` | YwRobot RGB LED 색상 출력 테스트 |
| `Vibration_Motor/Vibration_Motor.ino` | 진동 모터 ON/OFF 테스트 |
| `Vibration_Motor_PWM/Vibration_Motor_PWM.ino` | PWM을 이용한 진동 모터 세기 제어 테스트 |
| `Passive_Buzzer/Passive_Buzzer.ino` | 패시브 부저 주파수 출력 테스트 |

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
