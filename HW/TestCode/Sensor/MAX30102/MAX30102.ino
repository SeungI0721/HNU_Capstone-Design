// MAX30102 IR/RED 원시값을 확인해 심박·SpO2 측정 조건을 점검하는 테스트 스케치
#include <Arduino.h>
#include <Wire.h>
#include "MAX30105.h"

#define SDA_PIN 21
#define SCL_PIN 22

MAX30105 particleSensor;
bool sensorReady = false;

void setup() {
  Serial.begin(115200);
  delay(1000);

  Wire.begin(SDA_PIN, SCL_PIN);
  Wire.setClock(100000);

  Serial.println();
  Serial.println("MAX30102 / SEN0344 raw PPG test");
  Serial.println("Wiring: VCC=3V3, GND=GND, SDA=GPIO21, SCL=GPIO22");

  sensorReady = particleSensor.begin(Wire, I2C_SPEED_STANDARD);
  if (!sensorReady) {
    Serial.println("MAX30102 not found at 0x57");
    return;
  }

  particleSensor.setup();
  particleSensor.setPulseAmplitudeRed(0x24);
  particleSensor.setPulseAmplitudeIR(0x24);
  particleSensor.setPulseAmplitudeGreen(0);

  Serial.println("MAX30102 ready");
}

void loop() {
  if (!sensorReady) {
    Serial.println("Waiting for MAX30102...");
    delay(1000);
    return;
  }

  long red = particleSensor.getRed();
  long ir = particleSensor.getIR();

  Serial.print("RED: ");
  Serial.print(red);
  Serial.print("  IR: ");
  Serial.print(ir);

  if (red < 5000 && ir < 5000) {
    Serial.print("  Hint: place finger on sensor");
  }

  Serial.println();
  delay(500);
}
