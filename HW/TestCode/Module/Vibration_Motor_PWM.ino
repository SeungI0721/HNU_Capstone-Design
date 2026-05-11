#include <Arduino.h>

#define MOTOR_PIN 23

#define MOTOR_PWM_CHANNEL 0
#define MOTOR_PWM_FREQ 5000
#define MOTOR_PWM_RESOLUTION 8  // 0~255

void setup() {
  Serial.begin(115200);

  ledcSetup(MOTOR_PWM_CHANNEL, MOTOR_PWM_FREQ, MOTOR_PWM_RESOLUTION);
  ledcAttachPin(MOTOR_PIN, MOTOR_PWM_CHANNEL);

  ledcWrite(MOTOR_PWM_CHANNEL, 0);

  Serial.println("Vibration Motor PWM Test Start");
}

void loop() {
  Serial.println("Weak vibration");
  ledcWrite(MOTOR_PWM_CHANNEL, 80);
  delay(1000);

  Serial.println("Medium vibration");
  ledcWrite(MOTOR_PWM_CHANNEL, 150);
  delay(1000);

  Serial.println("Strong vibration");
  ledcWrite(MOTOR_PWM_CHANNEL, 255);
  delay(1000);

  Serial.println("Motor OFF");
  ledcWrite(MOTOR_PWM_CHANNEL, 0);
  delay(1500);
}