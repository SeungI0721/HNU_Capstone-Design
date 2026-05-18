/* Smart Shield payload 모듈: Android 앱으로 보낼 BLE 센서 payload를 생성합니다. */
#pragma once

String buildFakePayload() {
  // 실제 센서 없이 앱 파서와 Firebase 흐름을 확인하기 위한 payload입니다.
  unsigned long t = millis() / 1000;
  float skinTemp = 36.4 + ((int)(t % 5) - 2) * 0.1;
  int hr = 82 + (int)(t % 9);
  int spo2 = 97 + (int)(t % 2);
  float envTemp = 30.0 + ((int)(t % 8)) * 0.2;
  int hum = 58 + (int)(t % 6);
  int lux = 8000 + (int)((t % 10) * 500);
  float ax = ((int)(t % 9) - 4) * 0.15;
  float ay = ((int)(t % 7) - 3) * 0.12;
  float az = 9.7 + ((int)(t % 5) - 2) * 0.08;

  String payload = "";
  if (USE_PACKET_MARKERS) {
    payload += "<START>";
  }

  payload += "ID:";
  payload += WORKER_ID;
  payload += ",TEMP:";
  payload += String(skinTemp, 1);
  payload += ",HR:";
  payload += String(hr);
  payload += ",SPO2:";
  payload += String(spo2);
  payload += ",ENV:";
  payload += String(envTemp, 1);
  payload += ",HUM:";
  payload += String(hum);
  payload += ",LUX:";
  payload += String(lux);
  payload += ",AX:";
  payload += String(ax, 2);
  payload += ",AY:";
  payload += String(ay, 2);
  payload += ",AZ:";
  payload += String(az, 2);
  payload += ",POSTURE:NORMAL";

  if (USE_PACKET_MARKERS) {
    payload += "<END>";
  }
  payload += "\n";
  return payload;
}

String buildPayload() {
  // 센서값을 읽고 누락값은 앱이 처리 가능한 안전 기본값으로 보완합니다.
  if (FAKE_DATA_TEST_MODE) {
    return buildFakePayload();
  }

  String temp = valueOrFallback(readSkinTemp(), FALLBACK_TEMP);
  String hr = valueOrFallback(readHeartRateForApp(), FALLBACK_HR);
  String spo2 = readSpo2ForApp();
  String env = valueOrFallback(readEnvTemp(), FALLBACK_ENV);
  String hum = valueOrFallback(readHumidity(), FALLBACK_HUM);
  String lux = valueOrFallback(readLux(), FALLBACK_LUX);
  MotionSample motion = readMotionSample();
  String ax = valueOrFallback(motion.ax, FALLBACK_AXIS);
  String ay = valueOrFallback(motion.ay, FALLBACK_AXIS);
  String az = valueOrFallback(motion.az, FALLBACK_AXIS);
  String posture = valueOrFallback(motion.posture, FALLBACK_POSTURE);

  String payload = "";
  if (USE_PACKET_MARKERS) {
    payload += "<START>";
  }

  payload += "ID:";
  payload += WORKER_ID;
  payload += ",TEMP:";
  payload += temp;
  payload += ",HR:";
  payload += hr;
  payload += ",SPO2:";
  payload += spo2;
  payload += ",ENV:";
  payload += env;
  payload += ",HUM:";
  payload += hum;
  payload += ",LUX:";
  payload += lux;
  payload += ",AX:";
  payload += ax;
  payload += ",AY:";
  payload += ay;
  payload += ",AZ:";
  payload += az;
  payload += ",POSTURE:";
  payload += posture;

  if (USE_PACKET_MARKERS) {
    payload += "<END>";
  }
  payload += "\n";

  if (payload.length() > 120) {
    Serial.print("[WARN] Payload too long: ");
    Serial.println(payload.length());
  }

  return payload;
}

void sendNotifyIfReady() {
  // 1초 주기로 줄바꿈이 포함된 payload를 Notify characteristic으로 전송합니다.
  unsigned long now = millis();
  if (now - lastNotifyMs < NOTIFY_INTERVAL_MS) {
    return;
  }
  lastNotifyMs = now;

  String payload = buildPayload();
  Serial.print("[NOTIFY] ");
  Serial.print(payload);

  if (notifyCharacteristic != nullptr) {
    notifyCharacteristic->setValue((uint8_t*)payload.c_str(), payload.length());
    if (bleConnected) {
      notifyCharacteristic->notify();
      Serial.println("[BLE NOTIFY] sent");
    } else {
      Serial.println("[BLE NOTIFY] skipped: not connected");
    }
  }
}
