// ESP32 I2C 버스에 연결된 장치 주소를 스캔하는 테스트 스케치
#include <Arduino.h>
#include <Wire.h>

#define SDA_PIN 21
#define SCL_PIN 22

void printKnownDevice(uint8_t address) {
  switch (address) {
    case 0x23:
    case 0x5C:
      Serial.print("  BH1750/GY-302");
      break;
    case 0x48:
    case 0x49:
      Serial.print("  MAX30205/Fever Click candidate");
      break;
    case 0x57:
      Serial.print("  MAX30102/SEN0344");
      break;
    case 0x68:
    case 0x69:
      Serial.print("  MPU6050/GY-521");
      break;
    case 0x76:
    case 0x77:
      Serial.print("  BME280");
      break;
    default:
      break;
  }
}

void setup() {
  Serial.begin(115200);
  delay(1000);

  Wire.begin(SDA_PIN, SCL_PIN);
  Wire.setClock(100000);

  Serial.println();
  Serial.println("ESP32 I2C scanner");
  Serial.println("Wiring: SDA=GPIO21, SCL=GPIO22");
}

void loop() {
  int count = 0;

  Serial.println("Scanning...");

  for (uint8_t address = 1; address < 127; address++) {
    Wire.beginTransmission(address);
    uint8_t error = Wire.endTransmission();

    if (error == 0) {
      Serial.print("I2C device found at 0x");
      if (address < 16) Serial.print("0");
      Serial.print(address, HEX);
      printKnownDevice(address);
      Serial.println();
      count++;
    }
  }

  Serial.print("Total devices: ");
  Serial.println(count);
  Serial.println("----------------------");
  delay(3000);
}
