// MAX30205/Fever Click 제외
#include <Arduino.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BME280.h>
#include <math.h>

// =====================================================
// ESP32 I2C Pins
// =====================================================
#define SDA_PIN 21
#define SCL_PIN 22

// =====================================================
// BME280
// =====================================================
Adafruit_BME280 bme;
bool bmeOk = false;
uint8_t bmeAddress = 0;

// =====================================================
// BH1750
// =====================================================
#define BH1750_ADDR_LOW   0x23
#define BH1750_ADDR_HIGH  0x5C
#define BH1750_ONE_TIME_H_RES_MODE 0x20

bool bh1750Ok = false;
uint8_t bh1750Address = 0;

// =====================================================
// MPU6050
// =====================================================
#define MPU6050_ADDR_LOW   0x68
#define MPU6050_ADDR_HIGH  0x69

#define MPU_REG_PWR_MGMT_1    0x6B
#define MPU_REG_ACCEL_XOUT_H  0x3B
#define MPU_REG_WHO_AM_I      0x75

bool mpuOk = false;
uint8_t mpuAddress = 0;

// =====================================================
// MAX30102
// =====================================================
#define MAX30102_ADDR 0x57

#define MAX30102_REG_INTR_STATUS_1  0x00
#define MAX30102_REG_INTR_STATUS_2  0x01
#define MAX30102_REG_FIFO_WR_PTR    0x04
#define MAX30102_REG_OVF_COUNTER    0x05
#define MAX30102_REG_FIFO_RD_PTR    0x06
#define MAX30102_REG_FIFO_DATA      0x07
#define MAX30102_REG_FIFO_CONFIG    0x08
#define MAX30102_REG_MODE_CONFIG    0x09
#define MAX30102_REG_SPO2_CONFIG    0x0A
#define MAX30102_REG_LED1_PA        0x0C
#define MAX30102_REG_LED2_PA        0x0D
#define MAX30102_REG_PART_ID        0xFF

bool max30102Ok = false;

// =====================================================
// Common I2C Helper Functions
// =====================================================
bool isI2CReady(uint8_t address) {
  Wire.beginTransmission(address);
  return Wire.endTransmission() == 0;
}

bool writeRegister8(uint8_t address, uint8_t reg, uint8_t value) {
  Wire.beginTransmission(address);
  Wire.write(reg);
  Wire.write(value);
  return Wire.endTransmission() == 0;
}

bool readRegister8(uint8_t address, uint8_t reg, uint8_t &value) {
  Wire.beginTransmission(address);
  Wire.write(reg);

  if (Wire.endTransmission(false) != 0) {
    return false;
  }

  if (Wire.requestFrom((int)address, 1) != 1) {
    return false;
  }

  value = Wire.read();
  return true;
}

bool readRegisters(uint8_t address, uint8_t reg, uint8_t *buffer, uint8_t length) {
  Wire.beginTransmission(address);
  Wire.write(reg);

  if (Wire.endTransmission(false) != 0) {
    return false;
  }

  if (Wire.requestFrom((int)address, (int)length) != length) {
    return false;
  }

  for (uint8_t i = 0; i < length; i++) {
    buffer[i] = Wire.read();
  }

  return true;
}

// =====================================================
// I2C Scanner
// =====================================================
void scanI2CDevices() {
  Serial.println();
  Serial.println("========== I2C Scanner ==========");

  int count = 0;

  for (uint8_t address = 1; address < 127; address++) {
    Wire.beginTransmission(address);
    uint8_t error = Wire.endTransmission();

    if (error == 0) {
      Serial.print("Found I2C device at 0x");
      if (address < 16) Serial.print("0");
      Serial.println(address, HEX);
      count++;
    }
  }

  if (count == 0) {
    Serial.println("No I2C devices found");
  } else {
    Serial.print("Total devices found: ");
    Serial.println(count);
  }

  Serial.println("=================================");
  Serial.println();
}

// =====================================================
// BME280 Functions
// =====================================================
bool initBME280() {
  if (bme.begin(0x76, &Wire)) {
    bmeAddress = 0x76;
    return true;
  }

  if (bme.begin(0x77, &Wire)) {
    bmeAddress = 0x77;
    return true;
  }

  return false;
}

// =====================================================
// BH1750 Functions
// =====================================================
bool initBH1750() {
  if (isI2CReady(BH1750_ADDR_LOW)) {
    bh1750Address = BH1750_ADDR_LOW;
    return true;
  }

  if (isI2CReady(BH1750_ADDR_HIGH)) {
    bh1750Address = BH1750_ADDR_HIGH;
    return true;
  }

  return false;
}

float readBH1750Lux() {
  Wire.beginTransmission(bh1750Address);
  Wire.write(BH1750_ONE_TIME_H_RES_MODE);

  if (Wire.endTransmission() != 0) {
    return NAN;
  }

  delay(180);

  if (Wire.requestFrom((int)bh1750Address, 2) != 2) {
    return NAN;
  }

  uint16_t raw = Wire.read();
  raw <<= 8;
  raw |= Wire.read();

  return raw / 1.2;
}

// =====================================================
// MPU6050 Functions
// =====================================================
bool initMPU6050() {
  uint8_t who = 0;

  if (readRegister8(MPU6050_ADDR_LOW, MPU_REG_WHO_AM_I, who) && who == 0x68) {
    mpuAddress = MPU6050_ADDR_LOW;
  } else if (readRegister8(MPU6050_ADDR_HIGH, MPU_REG_WHO_AM_I, who) && who == 0x68) {
    mpuAddress = MPU6050_ADDR_HIGH;
  } else {
    return false;
  }

  // Wake up MPU6050
  if (!writeRegister8(mpuAddress, MPU_REG_PWR_MGMT_1, 0x00)) {
    return false;
  }

  delay(100);
  return true;
}

bool readMPU6050(float &ax, float &ay, float &az, float &gx, float &gy, float &gz, float &tempC) {
  uint8_t data[14];

  if (!readRegisters(mpuAddress, MPU_REG_ACCEL_XOUT_H, data, 14)) {
    return false;
  }

  int16_t accX = (int16_t)((data[0] << 8) | data[1]);
  int16_t accY = (int16_t)((data[2] << 8) | data[3]);
  int16_t accZ = (int16_t)((data[4] << 8) | data[5]);

  int16_t tempRaw = (int16_t)((data[6] << 8) | data[7]);

  int16_t gyroX = (int16_t)((data[8] << 8) | data[9]);
  int16_t gyroY = (int16_t)((data[10] << 8) | data[11]);
  int16_t gyroZ = (int16_t)((data[12] << 8) | data[13]);

  // Default sensitivity after reset:
  // Accel: ±2g  -> 16384 LSB/g
  // Gyro : ±250 deg/s -> 131 LSB/(deg/s)
  ax = accX / 16384.0;
  ay = accY / 16384.0;
  az = accZ / 16384.0;

  gx = gyroX / 131.0;
  gy = gyroY / 131.0;
  gz = gyroZ / 131.0;

  tempC = tempRaw / 340.0 + 36.53;

  return true;
}

// =====================================================
// MAX30102 Functions
// =====================================================
bool initMAX30102() {
  uint8_t partId = 0;

  if (!readRegister8(MAX30102_ADDR, MAX30102_REG_PART_ID, partId)) {
    return false;
  }

  Serial.print("MAX30102 PART_ID: 0x");
  Serial.println(partId, HEX);

  // MAX30102 PART_ID is commonly 0x15
  if (partId != 0x15) {
    Serial.println("Warning: PART_ID is not 0x15. Check module type.");
  }

  // Reset
  writeRegister8(MAX30102_ADDR, MAX30102_REG_MODE_CONFIG, 0x40);
  delay(100);

  // Clear FIFO pointers
  writeRegister8(MAX30102_ADDR, MAX30102_REG_FIFO_WR_PTR, 0x00);
  writeRegister8(MAX30102_ADDR, MAX30102_REG_OVF_COUNTER, 0x00);
  writeRegister8(MAX30102_ADDR, MAX30102_REG_FIFO_RD_PTR, 0x00);

  // FIFO config:
  // sample average = 4, FIFO rollover disabled, almost full = 17
  writeRegister8(MAX30102_ADDR, MAX30102_REG_FIFO_CONFIG, 0x4F);

  // SpO2 mode: Red + IR
  writeRegister8(MAX30102_ADDR, MAX30102_REG_MODE_CONFIG, 0x03);

  // SpO2 config:
  // ADC range 4096nA, sample rate 100Hz, pulse width 411us
  writeRegister8(MAX30102_ADDR, MAX30102_REG_SPO2_CONFIG, 0x27);

  // LED pulse amplitude
  // 너무 낮으면 값이 거의 안 나오고, 너무 높으면 포화될 수 있음
  writeRegister8(MAX30102_ADDR, MAX30102_REG_LED1_PA, 0x24);
  writeRegister8(MAX30102_ADDR, MAX30102_REG_LED2_PA, 0x24);

  // Clear interrupt status registers
  uint8_t dummy;
  readRegister8(MAX30102_ADDR, MAX30102_REG_INTR_STATUS_1, dummy);
  readRegister8(MAX30102_ADDR, MAX30102_REG_INTR_STATUS_2, dummy);

  return true;
}

bool readMAX30102Raw(uint32_t &redRaw, uint32_t &irRaw) {
  uint8_t data[6];

  if (!readRegisters(MAX30102_ADDR, MAX30102_REG_FIFO_DATA, data, 6)) {
    return false;
  }

  redRaw = ((uint32_t)data[0] << 16) | ((uint32_t)data[1] << 8) | data[2];
  redRaw &= 0x03FFFF;

  irRaw = ((uint32_t)data[3] << 16) | ((uint32_t)data[4] << 8) | data[5];
  irRaw &= 0x03FFFF;

  return true;
}

// =====================================================
// Setup
// =====================================================
void setup() {
  Serial.begin(115200);
  delay(1000);

  Serial.println();
  Serial.println("=============================================");
  Serial.println("Smart Shield Integrated I2C Verification Code");
  Serial.println("Sensors: BME280 + BH1750 + MPU6050 + MAX30102");
  Serial.println("Board  : ESP32 DevKit");
  Serial.println("I2C    : SDA GPIO21, SCL GPIO22");
  Serial.println("=============================================");

  Wire.begin(SDA_PIN, SCL_PIN);
  Wire.setClock(100000);

  scanI2CDevices();

  // BME280
  bmeOk = initBME280();
  Serial.print("BME280 init: ");
  if (bmeOk) {
    Serial.print("OK at 0x");
    Serial.println(bmeAddress, HEX);
  } else {
    Serial.println("FAILED");
  }

  // BH1750
  bh1750Ok = initBH1750();
  Serial.print("BH1750 init: ");
  if (bh1750Ok) {
    Serial.print("OK at 0x");
    Serial.println(bh1750Address, HEX);
  } else {
    Serial.println("FAILED");
  }

  // MPU6050
  mpuOk = initMPU6050();
  Serial.print("MPU6050 init: ");
  if (mpuOk) {
    Serial.print("OK at 0x");
    Serial.println(mpuAddress, HEX);
  } else {
    Serial.println("FAILED");
  }

  // MAX30102
  max30102Ok = isI2CReady(MAX30102_ADDR) && initMAX30102();
  Serial.print("MAX30102 init: ");
  if (max30102Ok) {
    Serial.println("OK at 0x57");
  } else {
    Serial.println("FAILED");
  }

  Serial.println("---------------------------------------------");
  Serial.println("Start reading sensors...");
  Serial.println("---------------------------------------------");
}

// =====================================================
// Loop
// =====================================================
void loop() {
  Serial.println();
  Serial.println("========== Sensor Readings ==========");

  // BME280
  if (bmeOk) {
    float envTemp = bme.readTemperature();
    float hum = bme.readHumidity();
    float pressure = bme.readPressure() / 100.0F;

    Serial.println("[BME280]");
    Serial.print("ENV Temp : ");
    Serial.print(envTemp, 2);
    Serial.println(" C");

    Serial.print("Humidity : ");
    Serial.print(hum, 2);
    Serial.println(" %");

    Serial.print("Pressure : ");
    Serial.print(pressure, 2);
    Serial.println(" hPa");
  } else {
    Serial.println("[BME280] Not available");
  }

  // BH1750
  if (bh1750Ok) {
    float lux = readBH1750Lux();

    Serial.println("[BH1750]");
    if (isnan(lux)) {
      Serial.println("Lux read failed");
    } else {
      Serial.print("Lux      : ");
      Serial.print(lux, 2);
      Serial.println(" lx");
    }
  } else {
    Serial.println("[BH1750] Not available");
  }

  // MPU6050
  if (mpuOk) {
    float ax, ay, az, gx, gy, gz, mpuTemp;

    Serial.println("[MPU6050]");
    if (readMPU6050(ax, ay, az, gx, gy, gz, mpuTemp)) {
      Serial.print("ACC[g]   X:");
      Serial.print(ax, 3);
      Serial.print(" Y:");
      Serial.print(ay, 3);
      Serial.print(" Z:");
      Serial.println(az, 3);

      Serial.print("GYRO[dps] X:");
      Serial.print(gx, 2);
      Serial.print(" Y:");
      Serial.print(gy, 2);
      Serial.print(" Z:");
      Serial.println(gz, 2);

      Serial.print("Chip Temp: ");
      Serial.print(mpuTemp, 2);
      Serial.println(" C");
    } else {
      Serial.println("MPU6050 read failed");
    }
  } else {
    Serial.println("[MPU6050] Not available");
  }

  // MAX30102
  if (max30102Ok) {
    uint32_t redRaw = 0;
    uint32_t irRaw = 0;

    Serial.println("[MAX30102]");
    if (readMAX30102Raw(redRaw, irRaw)) {
      Serial.print("RED Raw  : ");
      Serial.println(redRaw);

      Serial.print("IR Raw   : ");
      Serial.println(irRaw);

      if (irRaw < 5000 && redRaw < 5000) {
        Serial.println("Hint     : Finger may not be placed on sensor");
      } else {
        Serial.println("Hint     : Optical signal detected");
      }
    } else {
      Serial.println("MAX30102 FIFO read failed");
    }
  } else {
    Serial.println("[MAX30102] Not available");
  }

  Serial.println("=====================================");
  delay(1000);
}