#include <Arduino.h>

#define MOTOR_PIN 23

void setup() {
  Serial.begin(115200);
  delay(500);

  pinMode(MOTOR_PIN, OUTPUT);
  digitalWrite(MOTOR_PIN, LOW);

  Serial.println("Vibration motor ON/OFF test");
  Serial.println("GPIO23 must drive a MOSFET/transistor, not the motor directly");
}

void loop() {
  Serial.println("Motor ON");
  digitalWrite(MOTOR_PIN, HIGH);
  delay(500);

  Serial.println("Motor OFF");
  digitalWrite(MOTOR_PIN, LOW);
  delay(1500);
}
