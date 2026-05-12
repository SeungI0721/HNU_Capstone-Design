#include <Arduino.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BME280.h>
#include <BH1750.h>
#include "MAX30105.h"

#define SDA_PIN 21
#define SCL_PIN 22

#define MPU6050_ADDR_LOW 0x68
#define MPU6050_ADDR_HIGH 0x69
#define MPU_REG_PWR_MGMT_1 0x6B
#define MPU_REG_ACCEL_XOUT_H 0x3B
#define MPU_REG_WHO_AM_I 0x75

Adafruit_BME280 bme;
BH1750 lightMeter;
MAX30105 max30102;

bool bmeOk = false;
bool bh1750Ok = false;
bool max30102Ok = false;
bool max30205Ok = false;
bool mpuOk = false;

uint8_t bmeAddress = 0;
uint8_t max30205Address = 0;
uint8_t mpuAddress = 0;

bool i2cReady(uint8_t address) {
  Wire.beginTransmission(address);
  return Wire.endTransmission() == 0;
}

bool readRegister8(uint8_t address, uint8_t reg, uint8_t &value) {
  Wire.beginTransmission(address);
  Wire.write(reg);
  if (Wire.endTransmission(false) != 0) return false;
  if (Wire.requestFrom((int)address, 1) != 1) return false;
  value = Wire.read();
  return true;
}

bool writeRegister8(uint8_t address, uint8_t reg, uint8_t value) {
  Wire.beginTransmission(address);
  Wire.write(reg);
  Wire.write(value);
  return Wire.endTransmission() == 0;
}

void scanI2C() {
  Serial.println("I2C scan:");
  uint8_t count = 0;
  for (uint8_t address = 1; address < 127; address++) {
    if (i2cReady(address)) {
      Serial.print("  0x");
      if (address < 16) Serial.print("0");
      Serial.println(address, HEX);
      count++;
    }
  }
  Serial.print("Total I2C devices: ");
  Serial.println(count);
}

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

bool initMPU6050() {
  uint8_t who = 0;
  if (readRegister8(MPU6050_ADDR_LOW, MPU_REG_WHO_AM_I, who)) {
    mpuAddress = MPU6050_ADDR_LOW;
  } else if (readRegister8(MPU6050_ADDR_HIGH, MPU_REG_WHO_AM_I, who)) {
    mpuAddress = MPU6050_ADDR_HIGH;
  } else {
    return false;
  }

  writeRegister8(mpuAddress, MPU_REG_PWR_MGMT_1, 0x00);
  delay(100);
  return true;
}

bool readMPU6050(float &ax, float &ay, float &az, float &gx, float &gy, float &gz, float &tempC) {
  uint8_t data[14];
  Wire.beginTransmission(mpuAddress);
  Wire.write(MPU_REG_ACCEL_XOUT_H);
  if (Wire.endTransmission(false) != 0) return false;
  if (Wire.requestFrom((int)mpuAddress, 14) != 14) return false;

  for (uint8_t i = 0; i < 14; i++) {
    data[i] = Wire.read();
  }

  int16_t accX = (int16_t)((data[0] << 8) | data[1]);
  int16_t accY = (int16_t)((data[2] << 8) | data[3]);
  int16_t accZ = (int16_t)((data[4] << 8) | data[5]);
  int16_t tempRaw = (int16_t)((data[6] << 8) | data[7]);
  int16_t gyroX = (int16_t)((data[8] << 8) | data[9]);
  int16_t gyroY = (int16_t)((data[10] << 8) | data[11]);
  int16_t gyroZ = (int16_t)((data[12] << 8) | data[13]);

  ax = accX / 16384.0;
  ay = accY / 16384.0;
  az = accZ / 16384.0;
  gx = gyroX / 131.0;
  gy = gyroY / 131.0;
  gz = gyroZ / 131.0;
  tempC = tempRaw / 340.0 + 36.53;
  return true;
}

bool readMAX30205(float &temperatureC) {
  Wire.beginTransmission(max30205Address);
  Wire.write(0x00);
  if (Wire.endTransmission(false) != 0) return false;
  if (Wire.requestFrom((int)max30205Address, 2) != 2) return false;

  int16_t raw = (int16_t)((Wire.read() << 8) | Wire.read());
  temperatureC = raw * 0.00390625;
  return true;
}

void printInitStatus(const char *name, bool ok, uint8_t address) {
  Serial.print(name);
  Serial.print(": ");
  if (ok) {
    Serial.print("OK at 0x");
    Serial.println(address, HEX);
  } else {
    Serial.println("not found");
  }
}

void setup() {
  Serial.begin(115200);
  delay(1000);

  Wire.begin(SDA_PIN, SCL_PIN);
  Wire.setClock(100000);

  Serial.println();
  Serial.println("Smart Shield sensor integration test");
  Serial.println("I2C: SDA=GPIO21, SCL=GPIO22");

  scanI2C();

  bmeOk = initBME280();
  bh1750Ok = lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE, 0x23, &Wire);
  if (!bh1750Ok) {
    bh1750Ok = lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE, 0x5C, &Wire);
  }
  mpuOk = initMPU6050();
  max30102Ok = max30102.begin(Wire, I2C_SPEED_STANDARD);
  if (max30102Ok) {
    max30102.setup();
    max30102.setPulseAmplitudeRed(0x24);
    max30102.setPulseAmplitudeIR(0x24);
    max30102.setPulseAmplitudeGreen(0);
  }

  if (i2cReady(0x48)) {
    max30205Address = 0x48;
    max30205Ok = true;
  } else if (i2cReady(0x49)) {
    max30205Address = 0x49;
    max30205Ok = true;
  }

  printInitStatus("BME280", bmeOk, bmeAddress);
  Serial.println(bh1750Ok ? "BH1750: OK" : "BH1750: not found");
  printInitStatus("MPU6050", mpuOk, mpuAddress);
  Serial.println(max30102Ok ? "MAX30102: OK at 0x57" : "MAX30102: not found");
  printInitStatus("MAX30205", max30205Ok, max30205Address);
}

void loop() {
  Serial.println();
  Serial.println("========== Sensor readings ==========");

  if (bmeOk) {
    Serial.print("[BME280] Temp=");
    Serial.print(bme.readTemperature(), 2);
    Serial.print(" C Humidity=");
    Serial.print(bme.readHumidity(), 2);
    Serial.print(" % Pressure=");
    Serial.print(bme.readPressure() / 100.0F, 2);
    Serial.println(" hPa");
  } else {
    Serial.println("[BME280] not available");
  }

  if (bh1750Ok) {
    Serial.print("[BH1750] Lux=");
    Serial.print(lightMeter.readLightLevel(), 2);
    Serial.println(" lx");
  } else {
    Serial.println("[BH1750] not available");
  }

  if (mpuOk) {
    float ax, ay, az, gx, gy, gz, mpuTemp;
    if (readMPU6050(ax, ay, az, gx, gy, gz, mpuTemp)) {
      Serial.print("[MPU6050] ACC[g] X=");
      Serial.print(ax, 3);
      Serial.print(" Y=");
      Serial.print(ay, 3);
      Serial.print(" Z=");
      Serial.print(az, 3);
      Serial.print(" GYRO[dps] X=");
      Serial.print(gx, 2);
      Serial.print(" Y=");
      Serial.print(gy, 2);
      Serial.print(" Z=");
      Serial.print(gz, 2);
      Serial.print(" Temp=");
      Serial.print(mpuTemp, 2);
      Serial.println(" C");
    } else {
      Serial.println("[MPU6050] read failed");
    }
  } else {
    Serial.println("[MPU6050] not available");
  }

  if (max30102Ok) {
    long red = max30102.getRed();
    long ir = max30102.getIR();
    Serial.print("[MAX30102] RED=");
    Serial.print(red);
    Serial.print(" IR=");
    Serial.println(ir);
  } else {
    Serial.println("[MAX30102] not available");
  }

  if (max30205Ok) {
    float skinTemp = NAN;
    if (readMAX30205(skinTemp)) {
      Serial.print("[MAX30205] Skin contact temp=");
      Serial.print(skinTemp, 2);
      Serial.println(" C");
    } else {
      Serial.println("[MAX30205] read failed");
    }
  } else {
    Serial.println("[MAX30205] not available");
  }

  Serial.println("=====================================");
  delay(1000);
}
