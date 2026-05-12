#include <Arduino.h>
#include <Wire.h>

#define SDA_PIN 21
#define SCL_PIN 22

#define MPU6050_ADDR_LOW 0x68
#define MPU6050_ADDR_HIGH 0x69
#define MPU_REG_PWR_MGMT_1 0x6B
#define MPU_REG_ACCEL_XOUT_H 0x3B
#define MPU_REG_WHO_AM_I 0x75

uint8_t mpuAddress = 0;
bool sensorReady = false;

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

bool writeRegister8(uint8_t address, uint8_t reg, uint8_t value) {
  Wire.beginTransmission(address);
  Wire.write(reg);
  Wire.write(value);
  return Wire.endTransmission() == 0;
}

bool readMotion(float &ax, float &ay, float &az, float &gx, float &gy, float &gz, float &tempC) {
  uint8_t data[14];

  Wire.beginTransmission(mpuAddress);
  Wire.write(MPU_REG_ACCEL_XOUT_H);
  if (Wire.endTransmission(false) != 0) {
    return false;
  }

  if (Wire.requestFrom((int)mpuAddress, 14) != 14) {
    return false;
  }

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

void setup() {
  Serial.begin(115200);
  delay(1000);

  Wire.begin(SDA_PIN, SCL_PIN);
  Wire.setClock(100000);

  Serial.println();
  Serial.println("MPU6050 / GY-521 motion sensor test");
  Serial.println("Wiring: VCC=3V3, GND=GND, SDA=GPIO21, SCL=GPIO22");

  uint8_t who = 0;
  if (readRegister8(MPU6050_ADDR_LOW, MPU_REG_WHO_AM_I, who)) {
    mpuAddress = MPU6050_ADDR_LOW;
  } else if (readRegister8(MPU6050_ADDR_HIGH, MPU_REG_WHO_AM_I, who)) {
    mpuAddress = MPU6050_ADDR_HIGH;
  }

  if (mpuAddress == 0) {
    Serial.println("MPU6050 not found at 0x68 or 0x69");
    return;
  }

  writeRegister8(mpuAddress, MPU_REG_PWR_MGMT_1, 0x00);
  delay(100);
  sensorReady = true;

  Serial.print("MPU6050 ready at 0x");
  Serial.println(mpuAddress, HEX);
}

void loop() {
  if (!sensorReady) {
    Serial.println("Waiting for MPU6050...");
    delay(1000);
    return;
  }

  float ax, ay, az, gx, gy, gz, tempC;
  if (!readMotion(ax, ay, az, gx, gy, gz, tempC)) {
    Serial.println("MPU6050 read failed");
    delay(1000);
    return;
  }

  Serial.print("ACC[g] X:");
  Serial.print(ax, 3);
  Serial.print(" Y:");
  Serial.print(ay, 3);
  Serial.print(" Z:");
  Serial.print(az, 3);
  Serial.print(" | GYRO[dps] X:");
  Serial.print(gx, 2);
  Serial.print(" Y:");
  Serial.print(gy, 2);
  Serial.print(" Z:");
  Serial.print(gz, 2);
  Serial.print(" | Temp:");
  Serial.print(tempC, 2);
  Serial.println(" C");

  delay(500);
}
