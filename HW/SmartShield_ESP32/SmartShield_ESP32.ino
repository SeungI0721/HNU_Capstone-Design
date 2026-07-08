/* Smart Shield ESP32 메인 스케치: 전역 설정, 상태값, setup, loop를 관리합니다.
  Smart Shield - ESP32 작업자 안전 웨어러블 노드
  보드: ESP32 Dev Module

  BLE 이름: SS_0001
  Notify payload 형식:
  ID:0001,TEMP:34.8,TEMP_VALID:1,TEMP_SOURCE:MEASURED,HR:102,SPO2:97,ENV:33.1,HUM:71,LUX:42000,AX:0.01,AY:0.02,AZ:9.80,POSTURE:NORMAL
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
// 기본 설정값
// =========================
const char* WORKER_ID = "0001";
const char* BLE_DEVICE_NAME = "SS_0001";

const char* SERVICE_UUID = "089fca17-755f-4578-b8af-ee5e32526b0f";
const char* NOTIFY_CHAR_UUID = "0000FFF1-0000-1000-8000-00805F9B34FB";
const char* WRITE_CHAR_UUID = "0000FFF2-0000-1000-8000-00805F9B34FB";

const uint8_t I2C_SDA_PIN = 21;
const uint8_t I2C_SCL_PIN = 22;

const uint8_t LED_R_PIN = 27;

const uint8_t VIBRATION_PIN = 23;    // 진동 모터는 MOSFET 또는 NPN 트랜지스터를 통해 구동합니다.
const uint8_t BUZZER_PIN = 18;       // 패시브 부저는 트랜지스터를 통한 PWM 구동을 권장합니다.

const uint32_t SERIAL_BAUD = 115200;
const uint32_t NOTIFY_INTERVAL_MS = 1000;
const bool USE_PACKET_MARKERS = false;
const bool FAKE_DATA_TEST_MODE = false;
const bool USE_APP_SAFE_FALLBACK_VALUES = true;  // 센서가 없어도 Android 파서가 깨지지 않도록 기본값을 사용합니다.
const uint16_t BLE_MTU_SIZE = 128;

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

const uint32_t MAX30102_MIN_IR = 50000;  // IR 손가락 접촉 판단 기준값입니다.
const uint32_t MAX30102_MIN_RED = 10000;  // RED는 IR보다 낮게 들어올 수 있어 별도 기준을 사용합니다.
const uint32_t MAX30102_MAX_RAW = 260000;  // 포화로 판단할 원시 신호 상한입니다.
const uint32_t MAX30102_SAMPLE_INTERVAL_MS = 10;  // 100Hz 설정에 맞춰 약 10ms마다 샘플링합니다.
const byte MAX30102_RED_LED_AMPLITUDE = 0x24;
const byte MAX30102_IR_LED_AMPLITUDE = 0x24;
const byte SPO2_BUFFER_SIZE = 100;
const byte MAX30102_MISSING_RESET_COUNT = 3;
const uint32_t FINGER_STABLE_MS = 2000;
const uint32_t MAX30102_DEBUG_INTERVAL_MS = 1000;
const float BPM_JUMP_LIMIT = 35.0f;

// =========================
// 장치 객체와 상태값
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
  bool hrMeasured = false;
  bool spo2Measured = false;
  bool hasMeasuredValue = false;
  bool signalOutOfRange = false;
  uint32_t latestIr = 0;
  uint32_t latestRed = 0;
  uint32_t irBuffer[SPO2_BUFFER_SIZE] = {0};
  uint32_t redBuffer[SPO2_BUFFER_SIZE] = {0};
  byte bufferSpot = 0;
  byte bufferCount = 0;
  byte noFingerCount = 0;
  byte rates[8] = {0};
  byte rateSpot = 0;
  unsigned long lastSampleMs = 0;
  unsigned long fingerStableStartMs = 0;
  unsigned long lastDebugMs = 0;
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

#include "SmartShield_Utils.h"
#include "SmartShield_Outputs.h"

bool i2cDevicePresent(uint8_t address);
String readHeartRateForApp();
String readSpo2ForApp();

#include "SmartShield_Sensors.h"
#include "SmartShield_Payload.h"

#include "SmartShield_Ble.h"

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
