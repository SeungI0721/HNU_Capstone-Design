// 진동 모터 PWM 세기 제어를 단독 확인하는 테스트 스케치
#include <Arduino.h>

#define MOTOR_PIN 23
#define MOTOR_PWM_FREQ 5000
#define MOTOR_PWM_RESOLUTION 8

void motorWrite(uint8_t duty) {
  ledcWrite(MOTOR_PIN, duty);
}

void setup() {
  Serial.begin(115200);
  delay(500);

  ledcAttach(MOTOR_PIN, MOTOR_PWM_FREQ, MOTOR_PWM_RESOLUTION);
  motorWrite(0);

  Serial.println("Vibration motor PWM test");
  Serial.println("GPIO23 must drive a MOSFET/transistor, not the motor directly");
}

void loop() {
  Serial.println("Weak vibration");
  motorWrite(80);
  delay(1000);

  Serial.println("Medium vibration");
  motorWrite(150);
  delay(1000);

  Serial.println("Strong vibration");
  motorWrite(255);
  delay(1000);

  Serial.println("Motor OFF");
  motorWrite(0);
  delay(1500);
}
