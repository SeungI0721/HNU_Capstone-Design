/* Smart Shield 출력 모듈: LED, 진동 모터, 부저 경고 동작을 제어합니다. */
#pragma once

void setLedRaw(bool redOn, bool greenOn, bool blueOn) {
  digitalWrite(LED_R_PIN, COMMON_ANODE_LED ? !redOn : redOn);
  digitalWrite(LED_G_PIN, COMMON_ANODE_LED ? !greenOn : greenOn);
  digitalWrite(LED_B_PIN, COMMON_ANODE_LED ? !blueOn : blueOn);
}

void setLedSafe() {
  setLedRaw(false, true, false);
}

void setLedCaution() {
  setLedRaw(true, true, false);
}

void setLedDanger() {
  setLedRaw(true, false, false);
}

void setLedOff() {
  setLedRaw(false, false, false);
}

void startPattern(struct PatternState& pattern, uint8_t pulses, uint16_t onMs, uint16_t offMs, bool repeat, uint16_t toneHz = 0) {
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
  currentRisk = risk;
  emergencyLedOn = false;

  switch (risk) {
    case RISK_SAFE:
      setLedSafe();
      stopPattern(vibrationPattern);
      stopPattern(buzzerPattern);
      digitalWrite(VIBRATION_PIN, LOW);
      buzzerTone(0);
      Serial.println("[RISK] SAFE");
      break;

    case RISK_CAUTION:
      setLedCaution();
      startPattern(vibrationPattern, 1, 200, 120, false);
      startPattern(buzzerPattern, 1, 200, 120, false, 1000);
      Serial.println("[RISK] CAUTION");
      break;

    case RISK_DANGER:
      setLedDanger();
      startPattern(vibrationPattern, 1, 300, 700, true);
      startPattern(buzzerPattern, 1, 300, 700, true, 2000);
      Serial.println("[RISK] DANGER");
      break;

    case RISK_EMERGENCY:
      setLedDanger();
      startPattern(vibrationPattern, 1, 500, 300, true);
      startPattern(buzzerPattern, 1, 150, 150, true, 3000);
      Serial.println("[RISK] EMERGENCY");
      break;
  }
}

void updateLedForRisk() {
  if (currentRisk != RISK_EMERGENCY) {
    return;
  }

  unsigned long now = millis();
  if (now - lastBlinkMs >= 250) {
    lastBlinkMs = now;
    emergencyLedOn = !emergencyLedOn;
    if (emergencyLedOn) {
      setLedDanger();
    } else {
      setLedOff();
    }
  }
}

void initOutputs() {
  pinMode(LED_R_PIN, OUTPUT);
  pinMode(LED_G_PIN, OUTPUT);
  pinMode(LED_B_PIN, OUTPUT);
  pinMode(VIBRATION_PIN, OUTPUT);

  digitalWrite(VIBRATION_PIN, LOW);
  ledcAttach(BUZZER_PIN, 2000, 8);
  buzzerTone(0);
  applyRiskOutput(RISK_SAFE);
}
