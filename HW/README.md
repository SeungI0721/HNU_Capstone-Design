# Smart Shield HW

<<<<<<< HEAD
Smart Shield HW는 건설 현장 작업자의 **생체 데이터, 환경 데이터, 자세 데이터**를 수집하고,
위험도 단계에 따라 **RGB LED, 진동모터, 부저**로 작업자에게 즉시 경고를 제공하기 위한
ESP32 기반 PPE 웨어러블 하드웨어 구성입니다.
=======
ESP32-based PPE wearable hazard detection hardware. The device collects sensor data and drives RGB LED, vibration motor, and buzzer warnings according to risk commands from the Android app.

## Board Setting
>>>>>>> b978585 (Sensor Test)

Arduino IDE:

```text
<<<<<<< HEAD
[ Sensors ]
BME280 / BH1750 / MPU6050 / MAX30102 / MAX30205
        ↓
[ ESP32 DevKit ]
센서 데이터 수집 / BLE 통신 / 출력장치 제어
        ↓
[ Warning Output ]
RGB LED / Vibration Motor / Buzzer
````

---

## 2. Final HW Configuration

최종 하드웨어 구성은 다음과 같습니다.

| Category                | Component                          | Role                       |
| ----------------------- | ---------------------------------- | -------------------------- |
| Main Controller         | ESP32 DevKit                       | 센서 데이터 수집, BLE 통신, 출력장치 제어 |
| Environment Sensor      | BME280                             | 주변 온도, 습도, 기압 측정           |
| Light Sensor            | BH1750 / GY-302                    | 조도 측정, 직사광선 노출 가능성 추정      |
| Motion Sensor           | MPU6050 / GY-521                   | 자세 변화, 움직임, 낙상 추정          |
| Heart Sensor            | MAX30102 / SEN0344                 | 심박수 및 SpO₂ 추정              |
| Skin Temperature Sensor | MIKROE-2554 Fever Click / MAX30205 | 피부 접촉 온도 측정                |
| Visual Output           | YwRobot RGB LED Module             | 위험도 단계 색상 표시               |
| Haptic Output           | Cylindrical DC Vibration Motor     | 위험도 단계 진동 경고               |
| Sound Output            | Buzzer                             | 위험도 단계 단순 경고음 출력           |

---

## 3. Sensor Data Role

각 센서 데이터의 역할은 다음과 같습니다.

| Data Key | Sensor                 | Meaning          |
| -------- | ---------------------- | ---------------- |
| TEMP     | MAX30205 / Fever Click | 피부 접촉 온도         |
| HR       | MAX30102 / SEN0344     | 심박수              |
| SPO2     | MAX30102 / SEN0344     | 산소포화도 추정값, 선택 사용 |
| ENV      | BME280                 | 주변 환경 온도         |
| HUM      | BME280                 | 주변 습도            |
| PRESS    | BME280                 | 기압, 로그 또는 참고값    |
| LUX      | BH1750                 | 조도               |
| POSTURE  | MPU6050                | 자세 및 움직임 상태      |

BLE payload 예시:

```text
ID:0001,TEMP:36.5,HR:102,SPO2:97,ENV:33.1,HUM:71,LUX:45000,POSTURE:NORMAL
```

---

## 4. ESP32 Pin Assignment

### 4.1 I2C Bus

I2C 센서들은 하나의 공용 I2C 버스에 연결합니다.

| ESP32 Pin | Function   |
| --------- | ---------- |
| GPIO21    | SDA        |
| GPIO22    | SCL        |
| 3V3       | Sensor VCC |
| GND       | Sensor GND |

I2C 연결 센서:

```text
BME280
=======
Tools > Board > esp32 > ESP32 Dev Module
Tools > Port  > COM4
Serial Monitor baud rate: 115200
```

Arduino CLI:

```powershell
arduino-cli compile --fqbn esp32:esp32:esp32 "D:\HNU\HW\TestCode\Sensor\I2C_Seaner"
arduino-cli upload -p COM4 --fqbn esp32:esp32:esp32 "D:\HNU\HW\TestCode\Sensor\I2C_Seaner"
```

## Required Libraries

Installed/used libraries:

```text
Adafruit BME280 Library
Adafruit BusIO
Adafruit Unified Sensor
Adafruit MPU6050
>>>>>>> b978585 (Sensor Test)
BH1750
SparkFun MAX3010x Pulse and Proximity Sensor Library
```

Install command:

```powershell
arduino-cli lib install "Adafruit BME280 Library" "Adafruit Unified Sensor" "Adafruit MPU6050" "BH1750" "SparkFun MAX3010x Pulse and Proximity Sensor Library"
```

MAX30205 is tested with direct I2C register reads, so no separate MAX30205 library is required.

<<<<<<< HEAD
### 4.2 Final Pin Map
=======
## Pin Map
>>>>>>> b978585 (Sensor Test)

| Function | Component | ESP32 Pin |
| --- | --- | --- |
| I2C SDA | BME280, BH1750, MPU6050, MAX30102, MAX30205 | GPIO21 |
| I2C SCL | BME280, BH1750, MPU6050, MAX30102, MAX30205 | GPIO22 |
| RGB Red | YwRobot RGB LED R | GPIO27 |
| RGB Green | YwRobot RGB LED G | GPIO32 |
| RGB Blue | YwRobot RGB LED B | GPIO33 |
| Vibration Motor | MOSFET/transistor gate/base | GPIO23 |
| Buzzer | Passive/active buzzer signal | GPIO18 |

Use 3.3 V logic for sensor signals. Do not drive a vibration motor directly from an ESP32 GPIO; use a MOSFET or transistor and a flyback diode.

## I2C Address Checklist

| Component | Expected Address |
| --- | --- |
| BH1750 / GY-302 | 0x23 or 0x5C |
| MAX30205 / Fever Click | 0x48 or 0x49 |
| MAX30102 / SEN0344 | 0x57 |
| MPU6050 / GY-521 | 0x68 or 0x69 |
| BME280 | 0x76 or 0x77 |

Current board verification detected BME280, BH1750, MPU6050, and MAX30102. MAX30205 was not detected, so check its VCC, GND, SDA, SCL, and address configuration.

## Test Sketches

Use the folder versions below for Arduino IDE/CLI uploads:

```text
HW/TestCode/Sensor/I2C_Seaner/I2C_Seaner.ino
HW/TestCode/Sensor/BME280/BME280.ino
HW/TestCode/Sensor/BH1750/BH1750.ino
HW/TestCode/Sensor/MPU6050/MPU6050.ino
HW/TestCode/Sensor/MAX30102/MAX30102.ino
HW/TestCode/Sensor/MAX30205/MAX30205.ino
HW/TestCode/Module/YwRobot_RGB_LED/YwRobot_RGB_LED.ino
HW/TestCode/Module/Vibration_Motor/Vibration_Motor.ino
HW/TestCode/Module/Vibration_Motor_PWM/Vibration_Motor_PWM.ino
HW/TestCode/Module/Passive_Buzzer/Passive_Buzzer.ino
HW/TestCode/Integration_Test_Code/Integration_Test_Code.ino
HW/TestCode/Warning_Integration_Test_Code/Warning_Integration_Test_Code.ino
```

The older flat `.ino` files are also filled with the same test code for reference, but Arduino CLI compiles most reliably when the sketch folder name matches the `.ino` file name.

<<<<<<< HEAD
## 5. Sensor Wiring

### 5.1 BME280
=======
## Recommended Test Order

1. Upload `I2C_Seaner` and confirm visible I2C addresses.
2. Test each sensor sketch individually.
3. Upload `Integration_Test_Code` and confirm all connected sensors print values.
4. Test RGB LED, vibration motor, and buzzer individually.
5. Upload `Warning_Integration_Test_Code` only after confirming the motor driver and buzzer wiring are safe.
>>>>>>> b978585 (Sensor Test)

## Verified Result

Board: ESP32 Dev Module on COM4.

Compile verification passed for all folder-based sketches listed above.

Sensor integration upload and serial verification passed for:

```text
BME280   : temperature, humidity, pressure output OK
BH1750   : lux output OK
MPU6050  : accelerometer/gyro output OK
MAX30102 : RED/IR raw output OK
MAX30205 : not detected
```

Example serial output:

```text
[BME280] Temp=26.83 C Humidity=52.01 % Pressure=1000.16 hPa
[BH1750] Lux=181.67 lx
[MPU6050] ACC[g] X=0.019 Y=0.930 Z=-0.403
[MAX30102] RED=566 IR=1357
[MAX30205] not available
```
<<<<<<< HEAD

주의:

```text
BME280은 체온 센서가 아니다.
피부 근처나 조끼 안쪽에 배치하면 작업자 체열 때문에 환경 온도값이 왜곡될 수 있다.
```

---

### 5.2 BH1750 / GY-302

BH1750은 조도를 lux 단위로 측정합니다.

| BH1750 | ESP32  |
| ------ | ------ |
| VCC    | 3V3    |
| GND    | GND    |
| SDA    | GPIO21 |
| SCL    | GPIO22 |

예상 I2C 주소:

```text
0x23 또는 0x5C
```

사용 목적:

```text
LUX = 주변 조도
직사광선 / 그늘 여부 추정
```

주의:

```text
BH1750은 복사열을 측정하는 센서가 아니라 빛의 밝기를 측정하는 센서이다.
RGB LED 빛이 조도센서에 직접 들어가지 않도록 배치해야 한다.
```

---

### 5.3 MPU6050 / GY-521

MPU6050은 3축 가속도와 3축 자이로 데이터를 측정합니다.

| MPU6050 | ESP32            |
| ------- | ---------------- |
| VCC     | 3V3              |
| GND     | GND              |
| SDA     | GPIO21           |
| SCL     | GPIO22           |
| INT     | GPIO34, optional |

예상 I2C 주소:

```text
0x68 또는 0x69
```

사용 목적:

```text
POSTURE = NORMAL / WARNING / UNSTABLE / FALL / EMERGENCY
```

감지 항목:

```text
자세 변화
기울기
움직임
낙상 추정
움직임 없음
```

주의:

```text
MPU6050은 몸통과 함께 움직이는 단단한 위치에 고정해야 한다.
헐겁게 고정되면 작업자 움직임이 아니라 센서 자체 흔들림을 측정하게 된다.
진동모터와 가까이 배치하면 진동을 움직임으로 오인할 수 있으므로 물리적으로 분리한다.
```

---

### 5.4 MAX30102 / SEN0344

MAX30102는 PPG 방식으로 심박수와 산소포화도 추정값을 측정합니다.

I2C 방식 사용 기준:

| MAX30102 | ESP32  |
| -------- | ------ |
| VCC      | 3V3    |
| GND      | GND    |
| SDA      | GPIO21 |
| SCL      | GPIO22 |

예상 I2C 주소:

```text
0x57
```

사용 목적:

```text
HR = 심박수
SPO2 = 산소포화도 추정값
```

주의:

```text
MAX30102는 피부 밀착과 외부광 차단이 중요하다.
손가락 클립형 또는 귓불 클립형 구조가 적합하다.
조끼 몸통에 단순 부착하는 방식은 권장하지 않는다.
움직임이 큰 구간에서는 측정값 신뢰도가 낮아질 수 있다.
```

---

### 5.5 MAX30205 / MIKROE-2554 Fever Click

MAX30205 / Fever Click은 작업자의 피부 접촉 온도를 측정합니다.

현재 해당 부품은 아직 실물 테스트 전이지만,
최종 하드웨어 구성에는 포함하는 것으로 가정합니다.

| Fever Click / MAX30205 | ESP32            |
| ---------------------- | ---------------- |
| 3.3V                   | 3V3              |
| GND                    | GND              |
| SDA                    | GPIO21           |
| SCL                    | GPIO22           |
| OS/INT                 | GPIO35, optional |

예상 I2C 주소:

```text
0x48 또는 0x49 계열
```

사용 목적:

```text
TEMP = 피부 접촉 온도
```

주의:

```text
MAX30205는 심부체온을 직접 측정하는 센서가 아니다.
피부 접촉 온도 변화 추세를 확인하는 보조 지표로 사용한다.
센서가 피부와 안정적으로 닿아야 하며, 옷 위나 조끼 외부에 두면 체온 데이터로 사용하기 어렵다.
```

비고:

```text
MAX30205 / Fever Click은 아직 실물 검증 전이다.
실물 도착 후 I2C Scanner로 주소를 먼저 확인하고,
0x48 또는 0x49 계열 주소가 잡히는지 확인해야 한다.
```

---

## 6. Output Module Wiring

### 6.1 RGB LED Module

RGB LED는 위험도 단계를 색상으로 표시합니다.

| RGB LED      | ESP32     |
| ------------ | --------- |
| R            | GPIO27    |
| G            | GPIO32    |
| B            | GPIO33    |
| Common Anode | 3V3 또는 5V |

Common Anode 기준:

```text
GPIO LOW = ON
GPIO HIGH = OFF
```

위험도 색상:

```text
SAFE      = Green
CAUTION   = Yellow
DANGER    = Red
EMERGENCY = Red Blink
```

주의:

```text
각 색상 라인에는 220Ω~330Ω 저항 사용을 권장한다.
모듈에 내장 저항이 있는지 실물 보드에서 확인한다.
```

---

### 6.2 Vibration Motor

진동모터는 작업자에게 촉각 경고를 제공합니다.

진동모터는 ESP32 GPIO에 직접 연결하지 않고,
MOSFET 또는 NPN 트랜지스터를 통해 구동합니다.

권장 회로:

```text
ESP32 GPIO23
→ 100Ω~1kΩ Gate/Base 저항
→ MOSFET Gate 또는 NPN Base

Motor +
→ 3V 또는 3.3V

Motor -
→ MOSFET Drain 또는 NPN Collector

MOSFET Source 또는 NPN Emitter
→ GND

Motor 양단
→ 플라이백 다이오드 병렬 연결

Gate-GND
→ 100kΩ 풀다운 저항
```

주의:

```text
진동모터는 기동 전류가 크므로 GPIO 직접 연결 금지.
모터 역기전력 보호를 위해 플라이백 다이오드를 사용한다.
MPU6050과 물리적으로 떨어뜨린다.
```

---

### 6.3 Buzzer

부저는 DFPlayer Mini를 대체하는 단순 청각 경고 장치입니다.

Active Buzzer 모듈 사용 시:

| Buzzer     | ESP32     |
| ---------- | --------- |
| S / Signal | GPIO18    |
| +          | 3V3 또는 5V |
| -          | GND       |

Passive Buzzer 사용 시:

```text
ESP32 GPIO18
→ PWM 출력
→ Buzzer
```

주의:

```text
사용하는 부저가 Active Buzzer인지 Passive Buzzer인지 먼저 확인한다.
Active Buzzer는 ON/OFF 패턴으로 제어한다.
Passive Buzzer는 PWM 주파수로 제어한다.
```

---

## 7. Warning Output Pattern

ESP32는 작업자 앱에서 받은 위험도 명령에 따라 출력장치를 제어합니다.

| Risk Level | RGB LED   | Vibration Motor           | Buzzer             |
| ---------- | --------- | ------------------------- | ------------------ |
| SAFE       | Green     | OFF                       | OFF                |
| CAUTION    | Yellow    | Short vibration           | Short beep         |
| DANGER     | Red       | Repeated vibration        | Slow repeated beep |
| EMERGENCY  | Red blink | Strong repeated vibration | Fast repeated beep |

출력 패턴 예시:

```text
SAFE:
LED Green
Motor OFF
Buzzer OFF

CAUTION:
LED Yellow
Motor 200ms ON
Buzzer 200ms ON

DANGER:
LED Red
Motor 300ms ON / 700ms OFF repeat
Buzzer 300ms ON / 700ms OFF repeat

EMERGENCY:
LED Red blink
Motor 500ms ON / 300ms OFF repeat
Buzzer 150ms ON / 150ms OFF repeat
```

---

## 8. BLE Hardware Interface

ESP32는 BLE Peripheral / GATT Server로 동작합니다.

작업자 앱은 BLE Central / GATT Client로 동작합니다.

```text
ESP32 → Worker App: BLE Notify
Worker App → ESP32: BLE Write
```

BLE 장치명:

```text
SS_0001
```

작업자 ID는 다음 세 곳에서 동일하게 사용합니다.

```text
BLE Device Name: SS_0001
Sensor Payload ID: 0001
Firebase workerId: 0001
```

---

## 9. BLE UUID

Service UUID:

```text
089fca17-755f-4578-b8af-ee5e32526b0f
```

Sensor Notify Characteristic:

```text
0000FFF1-0000-1000-8000-00805F9B34FB
```

Control Write Characteristic:

```text
0000FFF2-0000-1000-8000-00805F9B34FB
```

CCCD:

```text
00002902-0000-1000-8000-00805F9B34FB
```

---

## 10. BLE Payload

ESP32는 1초 주기로 센서 데이터를 BLE Notify로 전송합니다.

기본 payload:

```text
ID:0001,TEMP:36.5,HR:102,ENV:33.1,HUM:71,LUX:45000,POSTURE:NORMAL
ID:0001,TEMP:36.5,HR:102,SPO2:97,ENV:33.1,HUM:71,LUX:45000,POSTURE:NORMAL
```

필드 설명:

| Key     | Meaning  |
| ------- | -------- |
| ID      | 작업자 ID   |
| TEMP    | 피부 접촉 온도 |
| HR      | 심박수      |
| SPO2    | 산소포화도    |
| ENV     | 주변 온도    |
| HUM     | 주변 습도    |
| LUX     | 조도       |
| POSTURE | 자세 상태    |

---

## 11. BLE Control Command

작업자 앱은 계산된 위험도 결과를 ESP32에 BLE Write로 전송합니다.

```text
RISK:SAFE
RISK:CAUTION
RISK:DANGER
RISK:EMERGENCY
```

ESP32는 해당 명령에 따라 RGB LED, 진동모터, 부저를 제어합니다.

---

## 12. Hardware Placement

| Component              | Recommended Position      | Reason             |
| ---------------------- | ------------------------- | ------------------ |
| ESP32 DevKit           | 조끼 등판 상단 또는 옆구리 보호 케이스 내부 | 센서 배선 분기 및 BLE 통신  |
| BME280                 | 조끼 외부 통풍 위치               | 실제 주변 공기 온습도 측정    |
| BH1750                 | 조끼 전면 상단 또는 어깨 외부         | 외부광 측정             |
| MPU6050                | 허리벨트, 가슴 중앙, 등판 상부        | 몸통 자세와 움직임 감지      |
| MAX30102               | 손가락 클립 또는 귓불 클립           | PPG 신호 안정성         |
| MAX30205 / Fever Click | 상완 안쪽, 겨드랑이 근처, 피부 접촉 패치  | 피부 접촉 온도 측정        |
| RGB LED                | 조끼 전면 또는 어깨               | 시각적 위험도 표시         |
| Vibration Motor        | 어깨끈, 가슴끈, 허리벨트 접촉부        | 작업자가 직접 느낄 수 있는 위치 |
| Buzzer                 | 조끼 전면 또는 어깨 외측            | 경고음 전달             |

---

## 13. Hardware Test Order

하드웨어 테스트는 다음 순서로 진행합니다.

```text
1. ESP32 업로드 및 Serial Monitor 확인
2. I2C Scanner 실행
3. BME280 단독 테스트
4. BH1750 단독 테스트
5. MPU6050 단독 테스트
6. MAX30102 단독 테스트
7. MAX30205 / Fever Click 단독 테스트
   - 부품 도착 후 진행
   - I2C Scanner로 주소 확인 필요
8. BME280 + BH1750 + MPU6050 + MAX30102 통합 I2C 테스트
9. MAX30205 포함 통합 I2C 테스트
10. RGB LED 단독 테스트
11. 진동모터 단독 테스트
12. 부저 단독 테스트
13. RGB LED + 진동모터 + 부저 통합 출력 테스트
14. BLE Notify / Write 연동 테스트
15. 착용 위치 기반 통합 테스트
```

---

## 14. I2C Address Checklist

| Component              | Expected Address    |
| ---------------------- | ------------------- |
| BME280                 | 0x76 or 0x77        |
| BH1750                 | 0x23 or 0x5C        |
| MPU6050                | 0x68 or 0x69        |
| MAX30102               | 0x57                |
| MAX30205 / Fever Click | 0x48 or 0x49 series |

주의:

```text
센서를 연결한 뒤 반드시 I2C Scanner로 실제 주소를 확인한다.
모듈 리비전이나 주소 핀 설정에 따라 주소가 달라질 수 있다.
```

---

## 15. Hardware Design Notes

```text
1. 센서 전원은 가능하면 3.3V로 통일한다.
2. ESP32 GPIO에 5V 신호를 직접 입력하지 않는다.
3. I2C 배선은 짧게 유지한다.
4. I2C 센서가 많으므로 풀업 저항 중복 여부를 확인한다.
5. 센서 하나씩 단독 테스트 후 통합한다.
6. MAX30205 / Fever Click은 부품 도착 후 I2C 주소부터 확인한다.
7. MPU6050은 단단히 고정한다.
8. MPU6050과 진동모터는 물리적으로 분리한다.
9. BH1750과 RGB LED는 물리적으로 분리한다.
10. BME280은 피부 근처가 아니라 외부 공기 통풍 위치에 둔다.
11. MAX30102는 손가락 또는 귓불 클립 구조로 사용한다.
12. MAX30205는 피부 접촉 구조가 필요하다.
13. 진동모터는 MOSFET 또는 트랜지스터로 구동한다.
14. 진동모터 양단에는 플라이백 다이오드를 사용한다.
15. RGB LED가 Common Anode인지 확인한다.
16. 부저가 Active인지 Passive인지 확인한다.
17. DFPlayer Mini와 MLX90614는 최종 구성에서 제외한다.
```

---

## 16. Development Roadmap

```text
1. 하드웨어 단품 테스트
2. I2C 센서 통합 테스트
3. 출력장치 통합 테스트
4. BLE Notify 데이터 전송 구현
5. BLE Write 제어 명령 수신 구현
6. 위험도별 RGB LED / 진동모터 / 부저 패턴 구현
7. MAX30205 / Fever Click 부품 도착 후 온도 센서 테스트
8. 센서 데이터 기반 payload 생성
9. 작업자 앱 연동 테스트
10. 착용 위치 및 배선 고정 테스트
11. 최종 시제품 통합 검증
```

---

## 17. Final Summary

```text
Smart Shield HW는 ESP32 DevKit을 중심으로 BME280, BH1750, MPU6050, MAX30102, MAX30205/Fever Click 센서를 통합하고, 위험도 단계에 따라 RGB LED, 진동모터, 부저로 작업자에게 경고를 제공하는 PPE 웨어러블 하드웨어이다. MLX90614 비접촉 온도센서와 DFPlayer Mini 음성 출력 모듈은 최종 구성에서 제외하며, 피부 접촉 온도는 MAX30205/Fever Click으로 측정하고 청각 경고는 단순 부저로 처리한다.
```
=======
>>>>>>> b978585 (Sensor Test)
