#include <Arduino.h>

#define LED_R 27
#define LED_G 32
#define LED_B 33
#define MOTOR_PIN 23
#define BUZZER_PIN 18

#define LED_ON LOW
#define LED_OFF HIGH
#define BUZZER_PWM_RESOLUTION 8

void setColor(bool red, bool green, bool blue) {
  digitalWrite(LED_R, red ? LED_ON : LED_OFF);
  digitalWrite(LED_G, green ? LED_ON : LED_OFF);
  digitalWrite(LED_B, blue ? LED_ON : LED_OFF);
}

void motorOn() {
  digitalWrite(MOTOR_PIN, HIGH);
}

void motorOff() {
  digitalWrite(MOTOR_PIN, LOW);
}

void buzzerTone(uint32_t frequency) {
  ledcAttach(BUZZER_PIN, frequency, BUZZER_PWM_RESOLUTION);
  ledcWrite(BUZZER_PIN, 128);
}

void buzzerOff() {
  ledcWrite(BUZZER_PIN, 0);
}

void allOff() {
  setColor(false, false, false);
  motorOff();
  buzzerOff();
}

void outputSafe() {
  Serial.println("[SAFE] Green / motor off / buzzer off");
  setColor(false, true, false);
  motorOff();
  buzzerOff();
  delay(2000);
}

void outputCaution() {
  Serial.println("[CAUTION] Yellow / short vibration / short beep");
  setColor(true, true, false);
  motorOn();
  buzzerTone(1000);
  delay(200);
  motorOff();
  buzzerOff();
  delay(1800);
}

void outputDanger() {
  Serial.println("[DANGER] Red / repeated vibration / repeated beep");
  setColor(true, false, false);

  for (uint8_t i = 0; i < 3; i++) {
    motorOn();
    buzzerTone(2000);
    delay(300);
    motorOff();
    buzzerOff();
    delay(700);
  }
}

void outputEmergency() {
  Serial.println("[EMERGENCY] Red blink / fast vibration / fast beep");

  for (uint8_t i = 0; i < 8; i++) {
    setColor(true, false, false);
    motorOn();
    buzzerTone(3000);
    delay(150);
    setColor(false, false, false);
    motorOff();
    buzzerOff();
    delay(150);
  }
}

void setup() {
  Serial.begin(115200);
  delay(500);

  pinMode(LED_R, OUTPUT);
  pinMode(LED_G, OUTPUT);
  pinMode(LED_B, OUTPUT);
  pinMode(MOTOR_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);

  ledcAttach(BUZZER_PIN, 1000, BUZZER_PWM_RESOLUTION);
  allOff();

  Serial.println("Smart Shield warning output integration test");
  Serial.println("RGB: R=27 G=32 B=33, motor=23, passive buzzer=18");
}

void loop() {
  outputSafe();
  outputCaution();
  outputDanger();
  outputEmergency();
  allOff();
  delay(2000);
}
