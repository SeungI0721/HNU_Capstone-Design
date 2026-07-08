// BH1750 조도 센서 I2C 연결과 lux 측정을 확인하는 테스트 스케치
#include <Arduino.h>
#include <Wire.h>
#include <BH1750.h>

#define SDA_PIN 21
#define SCL_PIN 22

BH1750 lightMeter;
bool sensorReady = false;

void setup() {
  Serial.begin(115200);
  delay(1000);

  Wire.begin(SDA_PIN, SCL_PIN);
  Wire.setClock(100000);

  Serial.println();
  Serial.println("BH1750 / GY-302 light sensor test");
  Serial.println("Wiring: VCC=3V3, GND=GND, SDA=GPIO21, SCL=GPIO22");

  sensorReady = lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE, 0x23, &Wire);
  if (!sensorReady) {
    sensorReady = lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE, 0x5C, &Wire);
  }

  Serial.println(sensorReady ? "BH1750 ready" : "BH1750 not found at 0x23 or 0x5C");
}

void loop() {
  if (!sensorReady) {
    Serial.println("Waiting for BH1750...");
    delay(1000);
    return;
  }

  float lux = lightMeter.readLightLevel();
  if (lux < 0) {
    Serial.println("BH1750 read failed");
  } else {
    Serial.print("Light: ");
    Serial.print(lux, 2);
    Serial.println(" lx");
  }

  delay(1000);
}
