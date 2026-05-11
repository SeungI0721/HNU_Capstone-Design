#include <Arduino.h>
#include <Wire.h>

#define SDA_PIN 21
#define SCL_PIN 22

void setup() {
  Serial.begin(115200);
  delay(500);

  Wire.begin(SDA_PIN, SCL_PIN);
  Wire.setClock(100000);

  Serial.println();
  Serial.println("ESP32 I2C Scanner Start");
}

void loop() {
  byte error;
  int count = 0;

  Serial.println("Scanning...");

  for (byte address = 1; address < 127; address++) {
    Wire.beginTransmission(address);
    error = Wire.endTransmission();

    if (error == 0) {
      Serial.print("I2C device found at 0x");
      if (address < 16) Serial.print("0");
      Serial.println(address, HEX);
      count++;
    }
  }

  if (count == 0) {
    Serial.println("No I2C devices found");
  } else {
    Serial.print("Total devices: ");
    Serial.println(count);
  }

  Serial.println("----------------------");
  delay(3000);
}

// 예상 출력
// I2C device found at 0x23  | BH1750
// I2C device found at 0x49  | MAX30205 / Fever Click
// I2C device found at 0x57  | MAX30102 / SEN0344
// I2C device found at 0x68  | MPU6050
// I2C device found at 0x76  | BME280