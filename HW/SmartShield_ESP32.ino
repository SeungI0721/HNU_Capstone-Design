/*
  Smart Shield - ESP32 PPE Wearable Node
  Board: ESP32 Dev Module

  BLE name: SS_0001
  Notify payload:
  ID:0001,TEMP:36.5,HR:102,SPO2:97,ENV:33.1,HUM:71,LUX:42000,POSTURE:NORMAL
*/

#include <Arduino.h>
#include <Wire.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <Adafruit_BME280.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <BH1750.h>
#include <MAX30105.h>
#include "heartRate.h"

// =========================
// Beginner-edit constants
// =========================
const char* WORKER_ID = "0001";
const char* BLE_DEVICE_NAME = "SS_0001";

const char* SERVICE_UUID = "089fca17-755f-4578-b8af-ee5e32526b0f";
const char* NOTIFY_CHAR_UUID = "0000FFF1-0000-1000-8000-00805F9B34FB";
const char* WRITE_CHAR_UUID = "0000FFF2-0000-1000-8000-00805F9B34FB";

const uint8_t I2C_SDA_PIN = 21;
const uint8_t I2C_SCL_PIN = 22;

const uint8_t LED_R_PIN = 27;
const uint8_t LED_G_PIN = 32;
const uint8_t LED_B_PIN = 33;
const bool COMMON_ANODE_LED = true;  // YwRobot common-anode modules usually need true.

const uint8_t VIBRATION_PIN = 23;    // Drive motor through MOSFET/NPN, never directly.
const uint8_t BUZZER_PIN = 18;       // Passive buzzer PWM through transistor is recommended.

const uint32_t SERIAL_BAUD = 115200;
const uint32_t NOTIFY_INTERVAL_MS = 1000;
const bool USE_PACKET_MARKERS = false;
const bool FAKE_DATA_TEST_MODE = false;
const bool USE_APP_SAFE_FALLBACK_VALUES = true;  // Keep Android parser alive if a sensor is missing.
const uint16_t BLE_MTU_SIZE = 128;

const char* FALLBACK_TEMP = "36.5";
const char* FALLBACK_HR = "82";
const char* FALLBACK_SPO2 = "98";
const char* FALLBACK_ENV = "28.5";
const char* FALLBACK_HUM = "55";
const char* FALLBACK_LUX = "8000";
const char* FALLBACK_POSTURE = "NORMAL";
const char* FALLBACK_AXIS = "0.00";

const uint8_t BME280_ADDR_PRIMARY = 0x76;
const uint8_t BME280_ADDR_SECONDARY = 0x77;
const uint8_t MPU6050_ADDR_PRIMARY = 0x68;
const uint8_t MPU6050_ADDR_SECONDARY = 0x69;
const uint8_t BH1750_ADDR_PRIMARY = 0x23;
const uint8_t BH1750_ADDR_SECONDARY = 0x5C;
const uint8_t MAX30102_ADDR = 0x57;
const uint8_t MAX30205_ADDR = 0x48;

const uint32_t MAX30102_MIN_IR = 50000;  // Raise/lower after testing contact quality.
const uint32_t MAX30102_SAMPLE_INTERVAL_MS = 20;

// =========================
// Devices and state
// =========================
Adafruit_BME280 bme;
Adafruit_MPU6050 mpu;
BH1750 lightMeter;
MAX30105 particleSensor;

BLEServer* bleServer = nullptr;
BLECharacteristic* notifyCharacteristic = nullptr;
bool bleConnected = false;
bool restartAdvertising = false;

bool bmeReady = false;
bool mpuReady = false;
bool mpuRawReady = false;
bool bh1750Ready = false;
bool max30102Ready = false;
bool max30205Ready = false;

enum RiskLevel {
  RISK_SAFE,
  RISK_CAUTION,
  RISK_DANGER,
  RISK_EMERGENCY
};

uint8_t currentRisk = RISK_SAFE;

unsigned long lastNotifyMs = 0;
unsigned long lastBlinkMs = 0;
bool emergencyLedOn = false;

struct PulseState {
  long lastBeatMs = 0;
  float bpm = 0;
  float avgBpm = 0;
  int latestHr = 0;
  int latestSpo2 = 0;
  bool fingerDetected = false;
  byte rates[8] = {0};
  byte rateSpot = 0;
  unsigned long lastSampleMs = 0;
} pulse;

struct MotionSample {
  String ax;
  String ay;
  String az;
  String posture;
};

struct PatternState {
  bool active = false;
  bool outputOn = false;
  uint8_t pulsesDone = 0;
  uint8_t pulseTarget = 0;
  uint16_t onMs = 0;
  uint16_t offMs = 0;
  uint16_t toneHz = 0;
  bool repeat = false;
  unsigned long nextToggleMs = 0;
};

PatternState vibrationPattern;
PatternState buzzerPattern;

// =========================
// Utility
// =========================
bool isInRange(float value, float minValue, float maxValue) {
  return !isnan(value) && value >= minValue && value <= maxValue;
}

String formatFloatOrEmpty(float value, float minValue, float maxValue, uint8_t decimals) {
  if (!isInRange(value, minValue, maxValue)) {
    return "";
  }
  return String(value, (unsigned int)decimals);
}

String formatIntOrEmpty(float value, int minValue, int maxValue) {
  if (!isInRange(value, minValue, maxValue)) {
    return "";
  }
  return String((int)round(value));
}

String valueOrFallback(const String& value, const char* fallbackValue) {
  if (value.length() > 0) {
    return value;
  }
  return USE_APP_SAFE_FALLBACK_VALUES ? String(fallbackValue) : String("");
}

bool i2cDevicePresent(uint8_t address);

void setLedRaw(bool redOn, bool greenOn, bool blueOn) {
  digitalWrite(LED_R_PIN, COMMON_ANODE_LED ? !redOn : redOn);
  digitalWrite(LED_G_PIN, COMMON_ANODE_LED ? !greenOn : greenOn);
  digitalWrite(LED_B_PIN, COMMON_ANODE_LED ? !blueOn : blueOn);
}

void setLedSafe() {
  setLedRaw(false, true, false);
}

void setLedCaution() {
  setLedRaw(true, true, false);
}

void setLedDanger() {
  setLedRaw(true, false, false);
}

void setLedOff() {
  setLedRaw(false, false, false);
}

void startPattern(struct PatternState& pattern, uint8_t pulses, uint16_t onMs, uint16_t offMs, bool repeat, uint16_t toneHz = 0) {
  pattern.active = true;
  pattern.outputOn = false;
  pattern.pulsesDone = 0;
  pattern.pulseTarget = pulses;
  pattern.onMs = onMs;
  pattern.offMs = offMs;
  pattern.repeat = repeat;
  pattern.toneHz = toneHz;
  pattern.nextToggleMs = 0;
}

void stopPattern(struct PatternState& pattern) {
  pattern.active = false;
  pattern.outputOn = false;
  pattern.pulsesDone = 0;
  pattern.nextToggleMs = 0;
}

void updateVibrationPattern() {
  if (!vibrationPattern.active) {
    digitalWrite(VIBRATION_PIN, LOW);
    return;
  }

  unsigned long now = millis();
  if (now < vibrationPattern.nextToggleMs) {
    return;
  }

  if (!vibrationPattern.outputOn) {
    digitalWrite(VIBRATION_PIN, HIGH);
    vibrationPattern.outputOn = true;
    vibrationPattern.nextToggleMs = now + vibrationPattern.onMs;
  } else {
    digitalWrite(VIBRATION_PIN, LOW);
    vibrationPattern.outputOn = false;
    vibrationPattern.pulsesDone++;
    if (!vibrationPattern.repeat && vibrationPattern.pulsesDone >= vibrationPattern.pulseTarget) {
      stopPattern(vibrationPattern);
      return;
    }
    if (vibrationPattern.repeat && vibrationPattern.pulsesDone >= vibrationPattern.pulseTarget) {
      vibrationPattern.pulsesDone = 0;
      vibrationPattern.nextToggleMs = now + 700;
    } else {
      vibrationPattern.nextToggleMs = now + vibrationPattern.offMs;
    }
  }
}

void buzzerTone(uint16_t hz) {
  if (hz == 0) {
    ledcWriteTone(BUZZER_PIN, 0);
    ledcWrite(BUZZER_PIN, 0);
  } else {
    ledcWriteTone(BUZZER_PIN, hz);
    ledcWrite(BUZZER_PIN, 128);
  }
}

void updateBuzzerPattern() {
  if (!buzzerPattern.active) {
    buzzerTone(0);
    return;
  }

  unsigned long now = millis();
  if (now < buzzerPattern.nextToggleMs) {
    return;
  }

  if (!buzzerPattern.outputOn) {
    buzzerTone(buzzerPattern.toneHz);
    buzzerPattern.outputOn = true;
    buzzerPattern.nextToggleMs = now + buzzerPattern.onMs;
  } else {
    buzzerTone(0);
    buzzerPattern.outputOn = false;
    buzzerPattern.pulsesDone++;
    if (!buzzerPattern.repeat && buzzerPattern.pulsesDone >= buzzerPattern.pulseTarget) {
      stopPattern(buzzerPattern);
      return;
    }
    if (buzzerPattern.repeat && buzzerPattern.pulsesDone >= buzzerPattern.pulseTarget) {
      buzzerPattern.pulsesDone = 0;
      buzzerPattern.nextToggleMs = now + 700;
    } else {
      buzzerPattern.nextToggleMs = now + buzzerPattern.offMs;
    }
  }
}

void applyRiskOutput(uint8_t risk) {
  currentRisk = risk;
  emergencyLedOn = false;

  switch (risk) {
    case RISK_SAFE:
      setLedSafe();
      stopPattern(vibrationPattern);
      stopPattern(buzzerPattern);
      digitalWrite(VIBRATION_PIN, LOW);
      buzzerTone(0);
      Serial.println("[RISK] SAFE");
      break;

    case RISK_CAUTION:
      setLedCaution();
      startPattern(vibrationPattern, 1, 200, 120, false);
      startPattern(buzzerPattern, 1, 200, 120, false, 1000);
      Serial.println("[RISK] CAUTION");
      break;

    case RISK_DANGER:
      setLedDanger();
      startPattern(vibrationPattern, 1, 300, 700, true);
      startPattern(buzzerPattern, 1, 300, 700, true, 2000);
      Serial.println("[RISK] DANGER");
      break;

    case RISK_EMERGENCY:
      setLedDanger();
      startPattern(vibrationPattern, 1, 500, 300, true);
      startPattern(buzzerPattern, 1, 150, 150, true, 3000);
      Serial.println("[RISK] EMERGENCY");
      break;
  }
}

void updateLedForRisk() {
  if (currentRisk != RISK_EMERGENCY) {
    return;
  }

  unsigned long now = millis();
  if (now - lastBlinkMs >= 250) {
    lastBlinkMs = now;
    emergencyLedOn = !emergencyLedOn;
    if (emergencyLedOn) {
      setLedDanger();
    } else {
      setLedOff();
    }
  }
}

bool readMax30205(float& tempC) {
  Wire.beginTransmission(MAX30205_ADDR);
  Wire.write(0x00);
  if (Wire.endTransmission(false) != 0) {
    return false;
  }

  if (Wire.requestFrom((int)MAX30205_ADDR, 2) != 2) {
    return false;
  }

  int16_t raw = ((int16_t)Wire.read() << 8) | Wire.read();
  tempC = raw * 0.00390625f;
  return true;
}

String readSkinTemp() {
  if (!max30205Ready) {
    return "";
  }

  float tempC = NAN;
  if (!readMax30205(tempC)) {
    static bool tempReadFailLogged = false;
    if (!tempReadFailLogged) {
      Serial.println("[SENSOR] MAX30205 READ FAILED - TEMP fallback enabled");
      tempReadFailLogged = true;
    }
    return "";
  }
  return formatFloatOrEmpty(tempC, 25.0, 45.0, 1);
}

String readEnvTemp() {
  if (!bmeReady) {
    return "";
  }

  float tempC = bme.readTemperature();
  return formatFloatOrEmpty(tempC, -10.0, 60.0, 1);
}

String readHumidity() {
  if (!bmeReady) {
    return "";
  }

  float humidity = bme.readHumidity();
  return formatIntOrEmpty(humidity, 0, 100);
}

void updateHeartSensor() {
  if (!max30102Ready) {
    pulse.fingerDetected = false;
    return;
  }

  unsigned long now = millis();
  if (now - pulse.lastSampleMs < MAX30102_SAMPLE_INTERVAL_MS) {
    return;
  }
  pulse.lastSampleMs = now;

  long irValue = particleSensor.getIR();
  long redValue = particleSensor.getRed();
  pulse.fingerDetected = irValue >= MAX30102_MIN_IR && redValue > 0;

  if (!pulse.fingerDetected) {
    return;
  }

  if (checkForBeat(irValue)) {
    long delta = now - pulse.lastBeatMs;
    pulse.lastBeatMs = now;

    if (delta > 0) {
      pulse.bpm = 60.0 / (delta / 1000.0);
      if (pulse.bpm >= 40 && pulse.bpm <= 220) {
        pulse.rates[pulse.rateSpot++] = (byte)pulse.bpm;
        pulse.rateSpot %= 8;

        int total = 0;
        int count = 0;
        for (byte i = 0; i < 8; i++) {
          if (pulse.rates[i] > 0) {
            total += pulse.rates[i];
            count++;
          }
        }
        if (count > 0) {
          pulse.avgBpm = (float)total / count;
          pulse.latestHr = (int)round(pulse.avgBpm);
        }
      }
    }
  }

  // 앱 연동용 참고값입니다. 의료용 SpO2가 아니라 red/IR 비율 기반의 간이 추정값입니다.
  float ratio = (float)redValue / (float)irValue;
  int estimatedSpo2 = (int)round(110.0f - 25.0f * ratio);
  if (estimatedSpo2 >= 70 && estimatedSpo2 <= 100) {
    pulse.latestSpo2 = estimatedSpo2;
  }
}

String readHeartRate() {
  if (!max30102Ready || !pulse.fingerDetected || pulse.latestHr <= 0) {
    return "";
  }
  return String(pulse.latestHr);
}

String readSpo2() {
  if (!max30102Ready || !pulse.fingerDetected || pulse.latestSpo2 <= 0) {
    return "";
  }
  return String(pulse.latestSpo2);
}

String readHeartRateForApp() {
  String hr = readHeartRate();
  if (hr.length() > 0) {
    return hr;
  }

  if (max30102Ready && pulse.fingerDetected) {
    return String(FALLBACK_HR);
  }

  return valueOrFallback("", FALLBACK_HR);
}

bool readMpuRawAccel(float& ax, float& ay, float& az) {
  uint8_t address = i2cDevicePresent(MPU6050_ADDR_PRIMARY) ? MPU6050_ADDR_PRIMARY : MPU6050_ADDR_SECONDARY;

  Wire.beginTransmission(address);
  Wire.write(0x3B);
  if (Wire.endTransmission(false) != 0) {
    return false;
  }

  if (Wire.requestFrom((int)address, 6) != 6) {
    return false;
  }

  int16_t rawAx = ((int16_t)Wire.read() << 8) | Wire.read();
  int16_t rawAy = ((int16_t)Wire.read() << 8) | Wire.read();
  int16_t rawAz = ((int16_t)Wire.read() << 8) | Wire.read();

  // MPU6050 기본 +/-2g 스케일 기준입니다. 앱에는 m/s^2 단위로 전달합니다.
  ax = ((float)rawAx / 16384.0f) * 9.80665f;
  ay = ((float)rawAy / 16384.0f) * 9.80665f;
  az = ((float)rawAz / 16384.0f) * 9.80665f;
  return true;
}

MotionSample readMotionSample() {
  MotionSample sample = { "", "", "", "NORMAL" };
  if (!mpuReady && !mpuRawReady) {
    return sample;
  }

  float ax = NAN;
  float ay = NAN;
  float az = NAN;
  float gyroTotal = 0.0f;

  if (mpuReady) {
    sensors_event_t accel;
    sensors_event_t gyro;
    sensors_event_t temp;
    mpu.getEvent(&accel, &gyro, &temp);
    ax = accel.acceleration.x;
    ay = accel.acceleration.y;
    az = accel.acceleration.z;
    gyroTotal = sqrt(
      gyro.gyro.x * gyro.gyro.x +
      gyro.gyro.y * gyro.gyro.y +
      gyro.gyro.z * gyro.gyro.z
    );
  } else if (!readMpuRawAccel(ax, ay, az)) {
    return sample;
  }

  sample.ax = formatFloatOrEmpty(ax, -80.0, 80.0, 2);
  sample.ay = formatFloatOrEmpty(ay, -80.0, 80.0, 2);
  sample.az = formatFloatOrEmpty(az, -80.0, 80.0, 2);

  float gForce = sqrt(ax * ax + ay * ay + az * az) / 9.80665f;
  float tiltDeg = atan2(sqrt(ax * ax + ay * ay), abs(az)) * 180.0f / PI;

  if (gForce > 2.6 || gForce < 0.35) {
    sample.posture = "FALL";
    return sample;
  }
  if (tiltDeg > 70 && gyroTotal > 2.5) {
    sample.posture = "UNSTABLE";
    return sample;
  }
  if (tiltDeg > 45 || gyroTotal > 3.5) {
    sample.posture = "WARNING";
    return sample;
  }
  sample.posture = "NORMAL";
  return sample;
}

String readPosture() {
  return readMotionSample().posture;
}

String readLux() {
  if (!bh1750Ready) {
    return "";
  }
  float lux = lightMeter.readLightLevel();
  if (!isInRange(lux, 0, 200000)) {
    return "";
  }
  return String((int)round(lux));
}

String buildFakePayload() {
  unsigned long t = millis() / 1000;
  float skinTemp = 36.4 + ((int)(t % 5) - 2) * 0.1;
  int hr = 82 + (int)(t % 9);
  int spo2 = 97 + (int)(t % 2);
  float envTemp = 30.0 + ((int)(t % 8)) * 0.2;
  int hum = 58 + (int)(t % 6);
  int lux = 8000 + (int)((t % 10) * 500);
  float ax = ((int)(t % 9) - 4) * 0.15;
  float ay = ((int)(t % 7) - 3) * 0.12;
  float az = 9.7 + ((int)(t % 5) - 2) * 0.08;

  String payload = "";
  if (USE_PACKET_MARKERS) {
    payload += "<START>";
  }

  payload += "ID:";
  payload += WORKER_ID;
  payload += ",TEMP:";
  payload += String(skinTemp, 1);
  payload += ",HR:";
  payload += String(hr);
  payload += ",SPO2:";
  payload += String(spo2);
  payload += ",ENV:";
  payload += String(envTemp, 1);
  payload += ",HUM:";
  payload += String(hum);
  payload += ",LUX:";
  payload += String(lux);
  payload += ",AX:";
  payload += String(ax, 2);
  payload += ",AY:";
  payload += String(ay, 2);
  payload += ",AZ:";
  payload += String(az, 2);
  payload += ",POSTURE:NORMAL";

  if (USE_PACKET_MARKERS) {
    payload += "<END>";
  }
  payload += "\n";
  return payload;
}

String buildPayload() {
  if (FAKE_DATA_TEST_MODE) {
    return buildFakePayload();
  }

  // 체온 센서 미연결 상태에서도 앱 연동 테스트가 끊기지 않도록 안전값을 사용한다.
  // MAX30205가 정상 연결되면 실제 측정값이 우선 사용되고, 실패할 때만 fallback이 들어간다.
  String temp = valueOrFallback(readSkinTemp(), FALLBACK_TEMP);
  String hr = valueOrFallback(readHeartRateForApp(), FALLBACK_HR);
  String spo2 = valueOrFallback(readSpo2(), FALLBACK_SPO2);
  String env = valueOrFallback(readEnvTemp(), FALLBACK_ENV);
  String hum = valueOrFallback(readHumidity(), FALLBACK_HUM);
  String lux = valueOrFallback(readLux(), FALLBACK_LUX);
  MotionSample motion = readMotionSample();
  String ax = valueOrFallback(motion.ax, FALLBACK_AXIS);
  String ay = valueOrFallback(motion.ay, FALLBACK_AXIS);
  String az = valueOrFallback(motion.az, FALLBACK_AXIS);
  String posture = valueOrFallback(motion.posture, FALLBACK_POSTURE);

  String payload = "";
  if (USE_PACKET_MARKERS) {
    payload += "<START>";
  }

  payload += "ID:";
  payload += WORKER_ID;
  payload += ",TEMP:";
  payload += temp;
  payload += ",HR:";
  payload += hr;
  payload += ",SPO2:";
  payload += spo2;
  payload += ",ENV:";
  payload += env;
  payload += ",HUM:";
  payload += hum;
  payload += ",LUX:";
  payload += lux;
  payload += ",AX:";
  payload += ax;
  payload += ",AY:";
  payload += ay;
  payload += ",AZ:";
  payload += az;
  payload += ",POSTURE:";
  payload += posture;

  if (USE_PACKET_MARKERS) {
    payload += "<END>";
  }
  payload += "\n";

  if (payload.length() > 120) {
    Serial.print("[WARN] Payload too long: ");
    Serial.println(payload.length());
  }

  return payload;
}

void sendNotifyIfReady() {
  unsigned long now = millis();
  if (now - lastNotifyMs < NOTIFY_INTERVAL_MS) {
    return;
  }
  lastNotifyMs = now;

  String payload = buildPayload();
  Serial.print("[NOTIFY] ");
  Serial.print(payload);

  if (notifyCharacteristic != nullptr) {
    notifyCharacteristic->setValue((uint8_t*)payload.c_str(), payload.length());
    if (bleConnected) {
      notifyCharacteristic->notify();
      Serial.println("[BLE NOTIFY] sent");
    } else {
      Serial.println("[BLE NOTIFY] skipped: not connected");
    }
  }
}

void handleBleCommand(String command) {
  command.trim();
  Serial.print("[BLE WRITE] Received command: ");
  Serial.println(command);

  if (command == "RISK:SAFE") {
    applyRiskOutput(RISK_SAFE);
  } else if (command == "RISK:CAUTION") {
    applyRiskOutput(RISK_CAUTION);
  } else if (command == "RISK:DANGER") {
    applyRiskOutput(RISK_DANGER);
  } else if (command == "RISK:EMERGENCY") {
    applyRiskOutput(RISK_EMERGENCY);
  } else if (command == "RISK:ERROR") {
    applyRiskOutput(RISK_CAUTION);
    Serial.println("[BLE WRITE] RISK:ERROR mapped to CAUTION output");
  } else {
    Serial.println("[BLE WRITE] Unknown command");
  }
}

class SmartShieldServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* server) override {
    bleConnected = true;
    Serial.println("[BLE] Android app connected");
    Serial.println("[BLE] Android must enable notification on FFF1 CCCD 0x2902");
  }

  void onDisconnect(BLEServer* server) override {
    bleConnected = false;
    restartAdvertising = true;
    Serial.println("[BLE] Disconnected, advertising will restart");
  }
};

class SmartShieldWriteCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* characteristic) override {
    String command = String(characteristic->getValue().c_str());
    handleBleCommand(command);
  }
};

void initBle() {
  BLEDevice::init(BLE_DEVICE_NAME);
  BLEDevice::setMTU(BLE_MTU_SIZE);
  bleServer = BLEDevice::createServer();
  bleServer->setCallbacks(new SmartShieldServerCallbacks());

  BLEService* service = bleServer->createService(SERVICE_UUID);

  notifyCharacteristic = service->createCharacteristic(
    NOTIFY_CHAR_UUID,
    BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY
  );
  notifyCharacteristic->addDescriptor(new BLE2902());

  BLECharacteristic* writeCharacteristic = service->createCharacteristic(
    WRITE_CHAR_UUID,
    BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR
  );
  writeCharacteristic->setCallbacks(new SmartShieldWriteCallbacks());

  service->start();

  BLEAdvertising* advertising = BLEDevice::getAdvertising();
  advertising->addServiceUUID(SERVICE_UUID);
  advertising->setScanResponse(true);
  advertising->setMinPreferred(0x06);
  advertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();

  Serial.print("[BLE] Advertising started as ");
  Serial.println(BLE_DEVICE_NAME);
  Serial.print("[BLE] Local MTU set to ");
  Serial.println(BLE_MTU_SIZE);
}

bool i2cDevicePresent(uint8_t address) {
  Wire.beginTransmission(address);
  return Wire.endTransmission() == 0;
}

void scanI2C() {
  Serial.println("[I2C] Scanning...");
  for (uint8_t addr = 1; addr < 127; addr++) {
    if (i2cDevicePresent(addr)) {
      Serial.print("[I2C] Found 0x");
      if (addr < 16) {
        Serial.print("0");
      }
      Serial.println(addr, HEX);
    }
  }
}

void initSensors() {
  Wire.begin(I2C_SDA_PIN, I2C_SCL_PIN);
  Wire.setClock(400000);
  scanI2C();

  bmeReady = bme.begin(BME280_ADDR_PRIMARY, &Wire);
  if (!bmeReady) {
    bmeReady = bme.begin(BME280_ADDR_SECONDARY, &Wire);
  }
  Serial.println(bmeReady ? "[SENSOR] BME280 OK" : "[SENSOR] BME280 NOT FOUND");

  mpuReady = mpu.begin(MPU6050_ADDR_PRIMARY, &Wire);
  if (!mpuReady) {
    mpuReady = mpu.begin(MPU6050_ADDR_SECONDARY, &Wire);
  }
  mpuRawReady = false;
  if (mpuReady) {
    mpu.setAccelerometerRange(MPU6050_RANGE_8_G);
    mpu.setGyroRange(MPU6050_RANGE_500_DEG);
    mpu.setFilterBandwidth(MPU6050_BAND_21_HZ);
  } else if (i2cDevicePresent(MPU6050_ADDR_PRIMARY) || i2cDevicePresent(MPU6050_ADDR_SECONDARY)) {
    uint8_t address = i2cDevicePresent(MPU6050_ADDR_PRIMARY) ? MPU6050_ADDR_PRIMARY : MPU6050_ADDR_SECONDARY;
    Wire.beginTransmission(address);
    Wire.write(0x6B);
    Wire.write(0x00);
    mpuRawReady = Wire.endTransmission() == 0;
    if (mpuRawReady) {
      Wire.beginTransmission(address);
      Wire.write(0x1C);
      Wire.write(0x00);
      Wire.endTransmission();
    }
  }
  if (mpuReady) {
    Serial.println("[SENSOR] MPU6050 OK");
  } else if (mpuRawReady) {
    Serial.println("[SENSOR] MPU6050 RAW OK");
  } else {
    Serial.println("[SENSOR] MPU6050 NOT FOUND");
  }

  bh1750Ready = false;
  if (i2cDevicePresent(BH1750_ADDR_PRIMARY)) {
    bh1750Ready = lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE, BH1750_ADDR_PRIMARY, &Wire);
  } else if (i2cDevicePresent(BH1750_ADDR_SECONDARY)) {
    bh1750Ready = lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE, BH1750_ADDR_SECONDARY, &Wire);
  }
  Serial.println(bh1750Ready ? "[SENSOR] BH1750 OK" : "[SENSOR] BH1750 NOT FOUND");

  max30102Ready = particleSensor.begin(Wire, I2C_SPEED_FAST);
  if (max30102Ready) {
    byte ledBrightness = 0x1F;
    byte sampleAverage = 4;
    byte ledMode = 2;
    int sampleRate = 100;
    int pulseWidth = 411;
    int adcRange = 4096;
    particleSensor.setup(ledBrightness, sampleAverage, ledMode, sampleRate, pulseWidth, adcRange);
    particleSensor.setPulseAmplitudeRed(0x1F);
    particleSensor.setPulseAmplitudeIR(0x1F);
    particleSensor.setPulseAmplitudeGreen(0);
  }
  Serial.println(max30102Ready ? "[SENSOR] MAX30102 OK" : "[SENSOR] MAX30102 NOT FOUND");

  max30205Ready = i2cDevicePresent(MAX30205_ADDR);
  Serial.println(max30205Ready ? "[SENSOR] MAX30205 OK" : "[SENSOR] MAX30205 NOT FOUND - TEMP fallback enabled");
}

void initOutputs() {
  pinMode(LED_R_PIN, OUTPUT);
  pinMode(LED_G_PIN, OUTPUT);
  pinMode(LED_B_PIN, OUTPUT);
  pinMode(VIBRATION_PIN, OUTPUT);

  digitalWrite(VIBRATION_PIN, LOW);
  ledcAttach(BUZZER_PIN, 2000, 8);
  buzzerTone(0);
  applyRiskOutput(RISK_SAFE);
}

void setup() {
  Serial.begin(SERIAL_BAUD);
  delay(800);

  Serial.println();
  Serial.println("==================================");
  Serial.println("Smart Shield ESP32 PPE Node Boot");
  Serial.println("Worker ID: 0001");
  Serial.println("BLE Name : SS_0001");
  Serial.print("Fake Data Test Mode: ");
  Serial.println(FAKE_DATA_TEST_MODE ? "ON" : "OFF");
  Serial.print("App Safe Fallback Values: ");
  Serial.println(USE_APP_SAFE_FALLBACK_VALUES ? "ON" : "OFF");
  Serial.println("==================================");

  initOutputs();
  initSensors();
  initBle();
}

void loop() {
  if (restartAdvertising) {
    restartAdvertising = false;
    delay(100);
    BLEDevice::startAdvertising();
    Serial.println("[BLE] Advertising restarted");
  }

  updateLedForRisk();
  updateVibrationPattern();
  updateBuzzerPattern();
  updateHeartSensor();
  sendNotifyIfReady();
}
