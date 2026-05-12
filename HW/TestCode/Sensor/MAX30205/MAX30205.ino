#include <Arduino.h>
#include <Wire.h>

#define SDA_PIN 21
#define SCL_PIN 22

const uint8_t MAX30205_ADDR_1 = 0x48;
const uint8_t MAX30205_ADDR_2 = 0x49;
uint8_t sensorAddress = 0;
bool sensorReady = false;

bool isI2CReady(uint8_t address) {
  Wire.beginTransmission(address);
  return Wire.endTransmission() == 0;
}

bool readTemperature(float &temperatureC) {
  Wire.beginTransmission(sensorAddress);
  Wire.write(0x00);
  if (Wire.endTransmission(false) != 0) {
    return false;
  }

  if (Wire.requestFrom((int)sensorAddress, 2) != 2) {
    return false;
  }

  int16_t raw = (int16_t)((Wire.read() << 8) | Wire.read());
  temperatureC = raw * 0.00390625;
  return true;
}

void setup() {
  Serial.begin(115200);
  delay(1000);

  Wire.begin(SDA_PIN, SCL_PIN);
  Wire.setClock(100000);

  Serial.println();
  Serial.println("MAX30205 / Fever Click temperature test");
  Serial.println("Wiring: VCC=3V3, GND=GND, SDA=GPIO21, SCL=GPIO22");

  if (isI2CReady(MAX30205_ADDR_1)) {
    sensorAddress = MAX30205_ADDR_1;
    sensorReady = true;
  } else if (isI2CReady(MAX30205_ADDR_2)) {
    sensorAddress = MAX30205_ADDR_2;
    sensorReady = true;
  }

  if (sensorReady) {
    Serial.print("MAX30205 ready at 0x");
    Serial.println(sensorAddress, HEX);
  } else {
    Serial.println("MAX30205 not found at 0x48 or 0x49");
  }
}

void loop() {
  if (!sensorReady) {
    Serial.println("Waiting for MAX30205...");
    delay(1000);
    return;
  }

  float temperatureC = NAN;
  if (readTemperature(temperatureC)) {
    Serial.print("Skin contact temp: ");
    Serial.print(temperatureC, 2);
    Serial.println(" C");
  } else {
    Serial.println("MAX30205 read failed");
  }

  delay(1000);
}
