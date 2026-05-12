# 1 "D:\\HNU\\HW\\TestCode\\Integration_Test_Code\\Integration_Test_Code.ino"
# 2 "D:\\HNU\\HW\\TestCode\\Integration_Test_Code\\Integration_Test_Code.ino" 2
# 3 "D:\\HNU\\HW\\TestCode\\Integration_Test_Code\\Integration_Test_Code.ino" 2
# 4 "D:\\HNU\\HW\\TestCode\\Integration_Test_Code\\Integration_Test_Code.ino" 2
# 5 "D:\\HNU\\HW\\TestCode\\Integration_Test_Code\\Integration_Test_Code.ino" 2
# 6 "D:\\HNU\\HW\\TestCode\\Integration_Test_Code\\Integration_Test_Code.ino" 2
# 7 "D:\\HNU\\HW\\TestCode\\Integration_Test_Code\\Integration_Test_Code.ino" 2
# 17 "D:\\HNU\\HW\\TestCode\\Integration_Test_Code\\Integration_Test_Code.ino"
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
  Serial0.println("I2C scan:");
  uint8_t count = 0;
  for (uint8_t address = 1; address < 127; address++) {
    if (i2cReady(address)) {
      Serial0.print("  0x");
      if (address < 16) Serial0.print("0");
      Serial0.println(address, 16);
      count++;
    }
  }
  Serial0.print("Total I2C devices: ");
  Serial0.println(count);
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
  if (readRegister8(0x68, 0x75, who)) {
    mpuAddress = 0x68;
  } else if (readRegister8(0x69, 0x75, who)) {
    mpuAddress = 0x69;
  } else {
    return false;
  }

  writeRegister8(mpuAddress, 0x6B, 0x00);
  delay(100);
  return true;
}

bool readMPU6050(float &ax, float &ay, float &az, float &gx, float &gy, float &gz, float &tempC) {
  uint8_t data[14];
  Wire.beginTransmission(mpuAddress);
  Wire.write(0x3B);
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
  Serial0.print(name);
  Serial0.print(": ");
  if (ok) {
    Serial0.print("OK at 0x");
    Serial0.println(address, 16);
  } else {
    Serial0.println("not found");
  }
}

void setup() {
  Serial0.begin(115200);
  delay(1000);

  Wire.begin(21, 22);
  Wire.setClock(100000);

  Serial0.println();
  Serial0.println("Smart Shield sensor integration test");
  Serial0.println("I2C: SDA=GPIO21, SCL=GPIO22");

  scanI2C();

  bmeOk = initBME280();
  bh1750Ok = lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE, 0x23, &Wire);
  if (!bh1750Ok) {
    bh1750Ok = lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE, 0x5C, &Wire);
  }
  mpuOk = initMPU6050();
  max30102Ok = max30102.begin(Wire, 100000);
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
  Serial0.println(bh1750Ok ? "BH1750: OK" : "BH1750: not found");
  printInitStatus("MPU6050", mpuOk, mpuAddress);
  Serial0.println(max30102Ok ? "MAX30102: OK at 0x57" : "MAX30102: not found");
  printInitStatus("MAX30205", max30205Ok, max30205Address);
}

void loop() {
  Serial0.println();
  Serial0.println("========== Sensor readings ==========");

  if (bmeOk) {
    Serial0.print("[BME280] Temp=");
    Serial0.print(bme.readTemperature(), 2);
    Serial0.print(" C Humidity=");
    Serial0.print(bme.readHumidity(), 2);
    Serial0.print(" % Pressure=");
    Serial0.print(bme.readPressure() / 100.0F, 2);
    Serial0.println(" hPa");
  } else {
    Serial0.println("[BME280] not available");
  }

  if (bh1750Ok) {
    Serial0.print("[BH1750] Lux=");
    Serial0.print(lightMeter.readLightLevel(), 2);
    Serial0.println(" lx");
  } else {
    Serial0.println("[BH1750] not available");
  }

  if (mpuOk) {
    float ax, ay, az, gx, gy, gz, mpuTemp;
    if (readMPU6050(ax, ay, az, gx, gy, gz, mpuTemp)) {
      Serial0.print("[MPU6050] ACC[g] X=");
      Serial0.print(ax, 3);
      Serial0.print(" Y=");
      Serial0.print(ay, 3);
      Serial0.print(" Z=");
      Serial0.print(az, 3);
      Serial0.print(" GYRO[dps] X=");
      Serial0.print(gx, 2);
      Serial0.print(" Y=");
      Serial0.print(gy, 2);
      Serial0.print(" Z=");
      Serial0.print(gz, 2);
      Serial0.print(" Temp=");
      Serial0.print(mpuTemp, 2);
      Serial0.println(" C");
    } else {
      Serial0.println("[MPU6050] read failed");
    }
  } else {
    Serial0.println("[MPU6050] not available");
  }

  if (max30102Ok) {
    long red = max30102.getRed();
    long ir = max30102.getIR();
    Serial0.print("[MAX30102] RED=");
    Serial0.print(red);
    Serial0.print(" IR=");
    Serial0.println(ir);
  } else {
    Serial0.println("[MAX30102] not available");
  }

  if (max30205Ok) {
    float skinTemp = 
# 248 "D:\\HNU\\HW\\TestCode\\Integration_Test_Code\\Integration_Test_Code.ino" 3
                    (__builtin_nanf(""))
# 248 "D:\\HNU\\HW\\TestCode\\Integration_Test_Code\\Integration_Test_Code.ino"
                       ;
    if (readMAX30205(skinTemp)) {
      Serial0.print("[MAX30205] Skin contact temp=");
      Serial0.print(skinTemp, 2);
      Serial0.println(" C");
    } else {
      Serial0.println("[MAX30205] read failed");
    }
  } else {
    Serial0.println("[MAX30205] not available");
  }

  Serial0.println("=====================================");
  delay(1000);
}
