# Module Test Code

출력 모듈 단독 테스트 폴더입니다. RGB LED, 진동 모터, 부저가 ESP32 핀 제어에 맞게 동작하는지 확인합니다.

## 파일 구성

| 파일 | 내용 |
| --- | --- |
| `YwRobot_RGB_LED/YwRobot_RGB_LED.ino` | YwRobot RGB LED 색상 출력 테스트입니다. |
| `Vibration_Motor/Vibration_Motor.ino` | 진동 모터 ON/OFF 단순 출력 테스트입니다. |
| `Vibration_Motor_PWM/Vibration_Motor_PWM.ino` | PWM을 이용한 진동 모터 세기 제어 테스트입니다. |
| `Passive_Buzzer/Passive_Buzzer.ino` | 패시브 부저 주파수 출력 테스트입니다. |
| `README.md` | 본 폴더 설명 문서입니다. |

## 공통 핀

| 모듈 | ESP32 핀 |
| --- | --- |
| RGB LED R | GPIO27 |
| RGB LED G | GPIO32 |
| RGB LED B | GPIO33 |
| 진동 모터 | GPIO23 |
| 부저 | GPIO18 |

## 주의 사항

```text
진동 모터는 ESP32 GPIO에 직접 연결하지 않는다.
MOSFET 또는 트랜지스터와 플라이백 다이오드를 사용한다.
RGB LED 모듈이 공통 애노드인지 공통 캐소드인지 확인한다.
패시브 부저는 PWM 주파수로 제어한다.
액티브 부저는 단순 ON/OFF 방식으로 제어한다.
```
