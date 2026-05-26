/* Smart Shield 출력 모듈: RED LED, 진동 모터, 부저 경고 동작 제어 */
#pragma once

const bool RED_LED_ACTIVE_HIGH = true;

void setRedLed(bool on) {
  // 단일 RED LED 출력
  digitalWrite(LED_R_PIN, RED_LED_ACTIVE_HIGH ? on : !on);
}

void setLedSafe() {
  // 정상 LED OFF
  setRedLed(false);
}

void setLedCaution() {
  // 경고 LED 깜빡임은 updateLedForRisk에서 처리
  setRedLed(false);
}

void setLedDanger() {
  // 위험 LED ON
  setRedLed(true);
}

void setLedOff() {
  // LED 전체 OFF
  setRedLed(false);
}

void startPattern(struct PatternState& pattern, uint8_t pulses, uint16_t onMs, uint16_t offMs, bool repeat, uint16_t toneHz = 0) {
  // 진동/부저 패턴 상태 시작
  pattern.active = true;
  pattern.outputOn = false;
  pattern.pulsesDone = 0;
  pattern.pulseTarget = pulses;
  pattern.onMs = onMs;
  pattern.offMs = offMs;
  pattern.repeat = repeat;
  pattern.toneHz = toneHz;
  pattern.nextToggleMs = 0;
}

void stopPattern(struct PatternState& pattern) {
  pattern.active = false;
  pattern.outputOn = false;
  pattern.pulsesDone = 0;
  pattern.nextToggleMs = 0;
}

void updateVibrationPattern() {
  // delay 없는 진동 패턴 갱신
  if (!vibrationPattern.active) {
    digitalWrite(VIBRATION_PIN, LOW);
    return;
  }

  unsigned long now = millis();
  if (now < vibrationPattern.nextToggleMs) {
    return;
  }

  if (!vibrationPattern.outputOn) {
    digitalWrite(VIBRATION_PIN, HIGH);
    vibrationPattern.outputOn = true;
    vibrationPattern.nextToggleMs = now + vibrationPattern.onMs;
  } else {
    digitalWrite(VIBRATION_PIN, LOW);
    vibrationPattern.outputOn = false;
    vibrationPattern.pulsesDone++;
    if (!vibrationPattern.repeat && vibrationPattern.pulsesDone >= vibrationPattern.pulseTarget) {
      stopPattern(vibrationPattern);
      return;
    }
    if (vibrationPattern.repeat && vibrationPattern.pulsesDone >= vibrationPattern.pulseTarget) {
      vibrationPattern.pulsesDone = 0;
      vibrationPattern.nextToggleMs = now + 700;
    } else {
      vibrationPattern.nextToggleMs = now + vibrationPattern.offMs;
    }
  }
}

void buzzerTone(uint16_t hz) {
  if (hz == 0) {
    ledcWriteTone(BUZZER_PIN, 0);
    ledcWrite(BUZZER_PIN, 0);
  } else {
    ledcWriteTone(BUZZER_PIN, hz);
    ledcWrite(BUZZER_PIN, 128);
  }
}

void updateBuzzerPattern() {
  // delay 없는 부저 패턴 갱신
  if (!buzzerPattern.active) {
    buzzerTone(0);
    return;
  }

  unsigned long now = millis();
  if (now < buzzerPattern.nextToggleMs) {
    return;
  }

  if (!buzzerPattern.outputOn) {
    buzzerTone(buzzerPattern.toneHz);
    buzzerPattern.outputOn = true;
    buzzerPattern.nextToggleMs = now + buzzerPattern.onMs;
  } else {
    buzzerTone(0);
    buzzerPattern.outputOn = false;
    buzzerPattern.pulsesDone++;
    if (!buzzerPattern.repeat && buzzerPattern.pulsesDone >= buzzerPattern.pulseTarget) {
      stopPattern(buzzerPattern);
      return;
    }
    if (buzzerPattern.repeat && buzzerPattern.pulsesDone >= buzzerPattern.pulseTarget) {
      buzzerPattern.pulsesDone = 0;
      buzzerPattern.nextToggleMs = now + 700;
    } else {
      buzzerPattern.nextToggleMs = now + buzzerPattern.offMs;
    }
  }
}

void applyRiskOutput(uint8_t risk) {
  // 위험 단계별 RED LED, 진동, 부저 적용
  currentRisk = risk;
  emergencyLedOn = false;

  switch (risk) {
    case RISK_SAFE:
      setLedSafe();
      stopPattern(vibrationPattern);
      stopPattern(buzzerPattern);
      digitalWrite(VIBRATION_PIN, LOW);
      buzzerTone(0);
      Serial.println("[RISK] SAFE - RED LED OFF");
      break;

    case RISK_CAUTION:
      setLedCaution();
      startPattern(vibrationPattern, 1, 200, 120, false);
      startPattern(buzzerPattern, 1, 200, 120, false, 1000);
      Serial.println("[RISK] CAUTION - RED LED BLINK");
      break;

    case RISK_DANGER:
      setLedDanger();
      startPattern(vibrationPattern, 1, 300, 700, true);
      startPattern(buzzerPattern, 1, 300, 700, true, 2000);
      Serial.println("[RISK] DANGER - RED LED ON");
      break;

    case RISK_EMERGENCY:
      setLedDanger();
      startPattern(vibrationPattern, 1, 500, 300, true);
      startPattern(buzzerPattern, 1, 150, 150, true, 3000);
      Serial.println("[RISK] EMERGENCY - RED LED ON");
      break;
  }
}

void updateLedForRisk() {
  if (currentRisk != RISK_CAUTION) {
    return;
  }

  unsigned long now = millis();
  if (now - lastBlinkMs >= 500) {
    lastBlinkMs = now;
    emergencyLedOn = !emergencyLedOn;
    setRedLed(emergencyLedOn);
  }
}

void initOutputs() {
  // 출력 핀 초기화
  pinMode(LED_R_PIN, OUTPUT);

  pinMode(VIBRATION_PIN, OUTPUT);

  digitalWrite(VIBRATION_PIN, LOW);
  ledcAttach(BUZZER_PIN, 2000, 8);
  buzzerTone(0);
  applyRiskOutput(RISK_SAFE);
}
