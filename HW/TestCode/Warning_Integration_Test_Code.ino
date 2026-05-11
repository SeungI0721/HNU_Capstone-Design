#include <Arduino.h>

// RGB LED
#define LED_R 27
#define LED_G 32
#define LED_B 33

// Output modules
#define MOTOR_PIN 23
#define BUZZER_PIN 18

// Common Anode RGB LED 기준
#define LED_ON  LOW
#define LED_OFF HIGH

// PWM channel
#define BUZZER_CHANNEL 1
#define BUZZER_RESOLUTION 8

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

void buzzerTone(int frequency) {
  ledcSetup(BUZZER_CHANNEL, frequency, BUZZER_RESOLUTION);
  ledcAttachPin(BUZZER_PIN, BUZZER_CHANNEL);
  ledcWrite(BUZZER_CHANNEL, 128);  // 50% duty
}

void buzzerOff() {
  ledcWrite(BUZZER_CHANNEL, 0);
}

void allOff() {
  setColor(false, false, false);
  motorOff();
  buzzerOff();
}

void outputSafe() {
  Serial.println("[SAFE] Green / Motor OFF / Buzzer OFF");

  setColor(false, true, false);
  motorOff();
  buzzerOff();

  delay(2000);
}

void outputCaution() {
  Serial.println("[CAUTION] Yellow / Short vibration / 1000Hz beep");

  setColor(true, true, false);

  motorOn();
  buzzerTone(1000);
  delay(200);

  motorOff();
  buzzerOff();
  delay(1800);
}

void outputDanger() {
  Serial.println("[DANGER] Red / Slow repeat vibration / 2000Hz beep");

  setColor(true, false, false);

  for (int i = 0; i < 3; i++) {
    motorOn();
    buzzerTone(2000);
    delay(300);

    motorOff();
    buzzerOff();
    delay(700);
  }
}

void outputEmergency() {
  Serial.println("[EMERGENCY] Red blink / Fast vibration / 3000Hz beep");

  for (int i = 0; i < 8; i++) {
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

  pinMode(LED_R, OUTPUT);
  pinMode(LED_G, OUTPUT);
  pinMode(LED_B, OUTPUT);

  pinMode(MOTOR_PIN, OUTPUT);

  ledcSetup(BUZZER_CHANNEL, 1000, BUZZER_RESOLUTION);
  ledcAttachPin(BUZZER_PIN, BUZZER_CHANNEL);
  buzzerOff();

  allOff();

  Serial.println("Smart Shield Output Module Test Start - Passive Buzzer");
}

void loop() {
  outputSafe();
  outputCaution();
  outputDanger();
  outputEmergency();

  allOff();
  delay(2000);
}