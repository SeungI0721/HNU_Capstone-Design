# Smart Shield

Smart Shield is an ESP32 and Android based PPE wearable project for construction-site hazard detection. The device collects environmental, biometric, light, and motion data, sends sensor payloads to an Android app, and receives risk commands that drive local warning outputs.

## Hardware Summary

| Part | Role |
| --- | --- |
| ESP32 DevKit | Main controller, BLE, sensor collection, warning output |
| BME280 | Ambient temperature, humidity, pressure |
| BH1750 / GY-302 | Light level in lux |
| MPU6050 / GY-521 | Motion, posture, fall/movement reference |
| MAX30102 / SEN0344 | PPG raw RED/IR data for heart-rate/SpO2 estimation |
| MAX30205 / Fever Click | Skin-contact temperature reference |
| YwRobot RGB LED | Visual warning |
| Vibration Motor | Haptic warning |
| Buzzer | Simple audible warning |

Excluded from the final hardware plan:

```text
MLX90614      -> replaced by skin-contact MAX30205/Fever Click
DFPlayer Mini -> replaced by simple buzzer warning
```

## ESP32 Setup

Arduino board type:

```text
ESP32 Dev Module
```

Arduino CLI FQBN:

```text
esp32:esp32:esp32
```

Default port during verification:

```text
COM4
```

## Test Code

All hardware test sketches are under:

```text
HW/TestCode
```

Use the folder-based sketches for upload. Example:

```powershell
arduino-cli compile --fqbn esp32:esp32:esp32 "D:\HNU\HW\TestCode\Integration_Test_Code"
arduino-cli upload -p COM4 --fqbn esp32:esp32:esp32 "D:\HNU\HW\TestCode\Integration_Test_Code"
```

See `HW/README.md` for detailed pin maps, library list, sketch list, and verification results.

## BLE Payload Draft

Sensor notification payload:

```text
ID:0001,TEMP:36.5,HR:102,SPO2:97,ENV:33.1,HUM:71,LUX:45000,POSTURE:NORMAL
```

Control command payload:

```text
RISK:SAFE
RISK:CAUTION
RISK:DANGER
RISK:EMERGENCY
```

## Warning Pattern

| Risk Level | LED | Vibration | Buzzer |
| --- | --- | --- | --- |
| SAFE | Green | Off | Off |
| CAUTION | Yellow | Short pulse | Short beep |
| DANGER | Red | Repeated pulse | Repeated beep |
| EMERGENCY | Red blink | Fast repeated pulse | Fast repeated beep |
