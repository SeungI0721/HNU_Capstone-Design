/* Smart Shield 센서 모듈: I2C 센서 초기화와 센서값 읽기를 담당합니다. */
#pragma once

bool readMax30205(float& tempC) {
  // MAX30205 온도 레지스터를 직접 읽어 피부 온도를 계산합니다.
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

struct SkinTempSample {
  String value;
  bool valid;
  const char* source;
};

SkinTempSample readSkinTempSample() {
  if (!max30205Ready) {
    Serial.println("[MAX30205] not detected, VALID=0");
    return { "0.0", false, "INVALID" };
  }

  float tempC = NAN;
  if (!readMax30205(tempC)) {
    Serial.println("[MAX30205] read failed, VALID=0");
    return { "0.0", false, "INVALID" };
  }

  String formattedTemp = formatFloatOrEmpty(tempC, -20.0, 80.0, 1);
  if (formattedTemp.length() == 0) {
    Serial.println("[MAX30205] invalid numeric range, VALID=0");
    return { "0.0", false, "INVALID" };
  }

  Serial.print("[MAX30205] TEMP=");
  Serial.print(tempC, 2);
  Serial.println(" C, VALID=1");
  return { formattedTemp, true, "MEASURED" };
}

String readSkinTemp() {
  SkinTempSample sample = readSkinTempSample();
  return sample.valid ? sample.value : "";
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

void resetPulseState() {
  pulse.lastBeatMs = 0;
  pulse.bpm = 0;
  pulse.avgBpm = 0;
  pulse.latestHr = 0;
  pulse.latestSpo2 = 0;
  pulse.hrMeasured = false;
  pulse.spo2Measured = false;
  pulse.hasMeasuredValue = false;
  pulse.signalOutOfRange = false;
  pulse.bufferSpot = 0;
  pulse.bufferCount = 0;
  pulse.rateSpot = 0;
  memset(pulse.rates, 0, sizeof(pulse.rates));
  memset(pulse.irBuffer, 0, sizeof(pulse.irBuffer));
  memset(pulse.redBuffer, 0, sizeof(pulse.redBuffer));
}

bool isValidFingerSignal(uint32_t irValue, uint32_t redValue) {
  bool signalEnough = irValue >= MAX30102_MIN_IR && redValue >= MAX30102_MIN_RED;
  bool saturated = irValue >= MAX30102_MAX_RAW || redValue >= MAX30102_MAX_RAW;
  return signalEnough && !saturated;
}

bool calculateSpo2FromBuffer() {
  if (pulse.bufferCount < SPO2_BUFFER_SIZE) {
    return false;
  }

  uint32_t redMin = 0xFFFFFFFFUL;
  uint32_t redMax = 0;
  uint32_t irMin = 0xFFFFFFFFUL;
  uint32_t irMax = 0;
  double redSum = 0;
  double irSum = 0;

  for (byte i = 0; i < SPO2_BUFFER_SIZE; i++) {
    uint32_t red = pulse.redBuffer[i];
    uint32_t ir = pulse.irBuffer[i];
    redMin = min(redMin, red);
    redMax = max(redMax, red);
    irMin = min(irMin, ir);
    irMax = max(irMax, ir);
    redSum += red;
    irSum += ir;
  }

  float redDc = redSum / SPO2_BUFFER_SIZE;
  float irDc = irSum / SPO2_BUFFER_SIZE;
  float redAc = redMax - redMin;
  float irAc = irMax - irMin;

  if (redDc <= 0 || irDc <= 0 || redAc <= 0 || irAc <= 0) {
    return false;
  }

  float ratio = (redAc / redDc) / (irAc / irDc);
  if (isnan(ratio) || ratio < 0.4f || ratio > 3.0f) {
    return false;
  }

  // 의료용 계산이 아니라 앱 시연용 간이 SpO2 추정값입니다.
  int estimatedSpo2 = (int)round(110.0f - 25.0f * ratio);
  pulse.latestSpo2 = constrain(estimatedSpo2, 70, 100);
  pulse.spo2Measured = true;
  pulse.hasMeasuredValue = pulse.hrMeasured && pulse.spo2Measured;
  return true;
}

void printMax30102Debug() {
  unsigned long now = millis();
  if (now - pulse.lastDebugMs < MAX30102_DEBUG_INTERVAL_MS) {
    return;
  }
  pulse.lastDebugMs = now;

  if (!max30102Ready) {
    Serial.println("[MAX30102] not available, source=FALLBACK");
    return;
  }

  if (!pulse.fingerDetected) {
    Serial.println("[MAX30102] Finger not detected");
  }
  if (pulse.signalOutOfRange) {
    Serial.println("[MAX30102] Signal too weak or saturated");
  }

  Serial.print("[MAX30102] IR=");
  Serial.print(pulse.latestIr);
  Serial.print(", RED=");
  Serial.print(pulse.latestRed);
  Serial.print(", finger=");
  Serial.print(pulse.fingerDetected ? "YES" : "NO");
  Serial.print(", HR=");
  Serial.print(readHeartRateForApp());
  Serial.print(", SPO2=");
  Serial.print(readSpo2ForApp());
  Serial.print(", source=");
  Serial.println(pulse.hasMeasuredValue ? "MEASURED" : "FALLBACK");
}

void updateHeartSensor() {
  if (!max30102Ready) {
    pulse.fingerDetected = false;
    printMax30102Debug();
    return;
  }

  unsigned long now = millis();
  if (now - pulse.lastSampleMs < MAX30102_SAMPLE_INTERVAL_MS) {
    printMax30102Debug();
    return;
  }
  pulse.lastSampleMs = now;

  uint32_t irValue = particleSensor.getIR();
  uint32_t redValue = particleSensor.getRed();
  pulse.latestIr = irValue;
  pulse.latestRed = redValue;

  bool fingerNow = isValidFingerSignal(irValue, redValue);
  pulse.signalOutOfRange = !fingerNow;

  if (!fingerNow) {
    if (pulse.noFingerCount <= MAX30102_MISSING_RESET_COUNT) {
      pulse.noFingerCount++;
    }
    pulse.fingerDetected = false;
    if (pulse.noFingerCount == MAX30102_MISSING_RESET_COUNT) {
      resetPulseState();
      pulse.noFingerCount = MAX30102_MISSING_RESET_COUNT + 1;
    }
    printMax30102Debug();
    return;
  }

  pulse.noFingerCount = 0;

  if (pulse.signalOutOfRange) {
    printMax30102Debug();
    return;
  }

  if (!pulse.fingerDetected) {
    resetPulseState();
    pulse.fingerDetected = true;
    pulse.fingerStableStartMs = now;
  }

  pulse.redBuffer[pulse.bufferSpot] = redValue;
  pulse.irBuffer[pulse.bufferSpot] = irValue;
  pulse.bufferSpot = (pulse.bufferSpot + 1) % SPO2_BUFFER_SIZE;
  if (pulse.bufferCount < SPO2_BUFFER_SIZE) {
    pulse.bufferCount++;
  }

  if (now - pulse.fingerStableStartMs < FINGER_STABLE_MS) {
    printMax30102Debug();
    return;
  }

  if (checkForBeat(irValue)) {
    long delta = now - pulse.lastBeatMs;
    pulse.lastBeatMs = now;

    if (delta >= 273 && delta <= 1500) {
      float newBpm = 60.0f / (delta / 1000.0f);
      if (newBpm >= 40.0f && newBpm <= 220.0f) {
        if (pulse.avgBpm <= 0 || fabs(newBpm - pulse.avgBpm) <= BPM_JUMP_LIMIT) {
          pulse.rates[pulse.rateSpot++] = (byte)round(newBpm);
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
            pulse.bpm = newBpm;
            pulse.avgBpm = (float)total / count;
            pulse.latestHr = (int)round(pulse.avgBpm);
            pulse.hrMeasured = true;
            pulse.hasMeasuredValue = pulse.hrMeasured && pulse.spo2Measured;
          }
        }
      }
    }
  }

  calculateSpo2FromBuffer();
  printMax30102Debug();
}

String readHeartRate() {
  if (!max30102Ready || !pulse.fingerDetected || !pulse.hrMeasured || pulse.latestHr <= 0) {
    return "";
  }
  return String(pulse.latestHr);
}

String readSpo2() {
  if (!max30102Ready || !pulse.fingerDetected || !pulse.spo2Measured || pulse.latestSpo2 <= 0) {
    return "";
  }
  return String(pulse.latestSpo2);
}

String readSpo2ForApp() {
  String spo2 = readSpo2();
  if (spo2.length() > 0) {
    return spo2;
  }
  return valueOrFallback("", FALLBACK_SPO2);
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

  // MPU6050 원시 가속도값을 +/-2g 기준으로 m/s^2 단위로 변환합니다.
  ax = ((float)rawAx / 16384.0f) * 9.80665f;
  ay = ((float)rawAy / 16384.0f) * 9.80665f;
  az = ((float)rawAz / 16384.0f) * 9.80665f;
  return true;
}

MotionSample readMotionSample() {
  // MPU6050 가속도/자이로 값을 자세 상태와 축 payload 값으로 변환합니다.
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


bool i2cDevicePresent(uint8_t address) {
  Wire.beginTransmission(address);
  return Wire.endTransmission() == 0;
}

void scanI2C() {
  // 부팅 시 I2C 장치 주소를 출력해 배선과 센서 인식 상태를 확인합니다.
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
  // I2C 센서들을 순서대로 초기화하고 사용 가능 여부를 전역 상태값에 저장합니다.
  Wire.begin(I2C_SDA_PIN, I2C_SCL_PIN);
  Wire.setClock(100000);
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
    // Adafruit 드라이버 초기화가 실패해도 장치가 보이면 최소 레지스터 설정 후 가속도 원시값을 읽습니다.
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

  max30102Ready = particleSensor.begin(Wire, I2C_SPEED_STANDARD);
  if (max30102Ready) {
    particleSensor.setup();
    particleSensor.setPulseAmplitudeRed(MAX30102_RED_LED_AMPLITUDE);
    particleSensor.setPulseAmplitudeIR(MAX30102_IR_LED_AMPLITUDE);
    particleSensor.setPulseAmplitudeGreen(0);
    resetPulseState();
    Serial.print("[MAX30102] I2C=STANDARD, setup=DEFAULT, redAmp=0x");
    Serial.print(MAX30102_RED_LED_AMPLITUDE, HEX);
    Serial.print(", irAmp=0x");
    Serial.println(MAX30102_IR_LED_AMPLITUDE, HEX);
  }
  Serial.println(max30102Ready ? "[SENSOR] MAX30102 OK" : "[SENSOR] MAX30102 NOT FOUND");

  max30205Ready = i2cDevicePresent(MAX30205_ADDR);
  if (max30205Ready) {
    Serial.println("[MAX30205] detected at 0x48");
  } else {
    Serial.println("[MAX30205] not detected at 0x48");
  }
}
