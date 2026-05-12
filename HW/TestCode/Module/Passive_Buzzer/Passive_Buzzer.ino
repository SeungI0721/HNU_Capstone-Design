#include <Arduino.h>

#define BUZZER_PIN 18
#define BUZZER_PWM_RESOLUTION 8

void buzzerTone(uint32_t frequency) {
  ledcAttach(BUZZER_PIN, frequency, BUZZER_PWM_RESOLUTION);
  ledcWrite(BUZZER_PIN, 128);
}

void buzzerOff() {
  ledcWrite(BUZZER_PIN, 0);
}

void beep(const char *label, uint32_t frequency, uint16_t onMs, uint16_t offMs, uint8_t repeat) {
  Serial.println(label);
  for (uint8_t i = 0; i < repeat; i++) {
    buzzerTone(frequency);
    delay(onMs);
    buzzerOff();
    delay(offMs);
  }
}

void setup() {
  Serial.begin(115200);
  delay(500);

  pinMode(BUZZER_PIN, OUTPUT);
  ledcAttach(BUZZER_PIN, 1000, BUZZER_PWM_RESOLUTION);
  buzzerOff();

  Serial.println("Passive buzzer PWM test");
  Serial.println("Signal=GPIO18");
}

void loop() {
  beep("CAUTION beep: 1000 Hz", 1000, 200, 800, 1);
  beep("DANGER beep: 2000 Hz x3", 2000, 300, 300, 3);
  beep("EMERGENCY beep: 3000 Hz x8", 3000, 150, 150, 8);
  delay(2000);
}
