// BME280 환경 센서 I2C 연결과 온습도 측정을 확인하는 테스트 스케치
#include <Arduino.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BME280.h>

#define SDA_PIN 21
#define SCL_PIN 22

Adafruit_BME280 bme;
bool sensorReady = false;
uint8_t bmeAddress = 0;

void setup() {
  Serial.begin(115200);
  delay(1000);

  Wire.begin(SDA_PIN, SCL_PIN);
  Wire.setClock(100000);

  Serial.println();
  Serial.println("BME280 temperature / humidity / pressure test");
  Serial.println("Wiring: VCC=3V3, GND=GND, SDA=GPIO21, SCL=GPIO22");

  if (bme.begin(0x76, &Wire)) {
    sensorReady = true;
    bmeAddress = 0x76;
  } else if (bme.begin(0x77, &Wire)) {
    sensorReady = true;
    bmeAddress = 0x77;
  }

  if (sensorReady) {
    Serial.print("BME280 ready at 0x");
    Serial.println(bmeAddress, HEX);
  } else {
    Serial.println("BME280 not found at 0x76 or 0x77");
  }
}

void loop() {
  if (!sensorReady) {
    Serial.println("Waiting for BME280...");
    delay(1000);
    return;
  }

  Serial.print("Temp: ");
  Serial.print(bme.readTemperature(), 2);
  Serial.println(" C");

  Serial.print("Humidity: ");
  Serial.print(bme.readHumidity(), 2);
  Serial.println(" %");

  Serial.print("Pressure: ");
  Serial.print(bme.readPressure() / 100.0F, 2);
  Serial.println(" hPa");

  Serial.println("----------------");
  delay(1000);
}
