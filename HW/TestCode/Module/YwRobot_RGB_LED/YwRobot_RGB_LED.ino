// YwRobot RGB LED 모듈의 색상 출력 핀을 확인하는 테스트 스케치
#include <Arduino.h>

#define LED_R 27
#define LED_G 32
#define LED_B 33

#define LED_ON LOW
#define LED_OFF HIGH

void setColor(bool red, bool green, bool blue) {
  digitalWrite(LED_R, red ? LED_ON : LED_OFF);
  digitalWrite(LED_G, green ? LED_ON : LED_OFF);
  digitalWrite(LED_B, blue ? LED_ON : LED_OFF);
}

void showColor(const char *name, bool red, bool green, bool blue) {
  Serial.println(name);
  setColor(red, green, blue);
  delay(1000);
}

void setup() {
  Serial.begin(115200);
  delay(500);

  pinMode(LED_R, OUTPUT);
  pinMode(LED_G, OUTPUT);
  pinMode(LED_B, OUTPUT);
  setColor(false, false, false);

  Serial.println("YwRobot common-anode RGB LED test");
  Serial.println("R=GPIO27, G=GPIO32, B=GPIO33, common=3V3/5V");
}

void loop() {
  showColor("RED", true, false, false);
  showColor("GREEN", false, true, false);
  showColor("BLUE", false, false, true);
  showColor("YELLOW", true, true, false);
  showColor("WHITE", true, true, true);
  showColor("OFF", false, false, false);
  delay(1000);
}
