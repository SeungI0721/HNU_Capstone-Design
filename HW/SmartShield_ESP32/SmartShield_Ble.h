/* Smart Shield BLE 모듈: 광고, Notify, Write 명령 처리를 담당합니다. */
#pragma once

void handleBleCommand(String command) {
  command.trim();
  if (command.length() == 0) {
    return;
  }
  Serial.print("[BLE WRITE] Received command: ");
  Serial.println(command);

  // Android 앱이 계산한 위험도 명령을 로컬 출력 단계로 변환합니다.
  if (command == "RISK:SAFE") {
    applyRiskOutput(RISK_SAFE);
  } else if (command == "RISK:CAUTION") {
    applyRiskOutput(RISK_CAUTION);
  } else if (command == "RISK:DANGER") {
    applyRiskOutput(RISK_DANGER);
  } else if (command == "RISK:EMERGENCY") {
    applyRiskOutput(RISK_EMERGENCY);
  } else if (command == "RISK:ERROR") {
    applyRiskOutput(RISK_CAUTION);
    Serial.println("[BLE WRITE] RISK:ERROR mapped to CAUTION output");
  } else {
    Serial.println("[BLE WRITE] Unknown command");
  }
}

class SmartShieldServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* server) override {
    // Central 연결 상태를 payload 전송 조건으로 사용합니다.
    bleConnected = true;
    Serial.println("[BLE] Android app connected");
    Serial.println("[BLE] Android must enable notification on FFF1 CCCD 0x2902");
  }

  void onDisconnect(BLEServer* server) override {
    // 연결 해제 후 loop에서 광고를 재시작해 Android 재연결을 허용합니다.
    bleConnected = false;
    restartAdvertising = true;
    Serial.println("[BLE] Disconnected, advertising will restart");
  }
};

class SmartShieldWriteCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* characteristic) override {
    // FFF2 Write 값을 문자열 명령으로 해석합니다.
    String command = String(characteristic->getValue().c_str());
    handleBleCommand(command);
  }
};

void initBle() {
  // ESP32를 Peripheral/GATT Server로 시작하고 Notify/Write 특성을 등록합니다.
  BLEDevice::init(BLE_DEVICE_NAME);
  BLEDevice::setMTU(BLE_MTU_SIZE);
  bleServer = BLEDevice::createServer();
  bleServer->setCallbacks(new SmartShieldServerCallbacks());

  BLEService* service = bleServer->createService(SERVICE_UUID);

  notifyCharacteristic = service->createCharacteristic(
    NOTIFY_CHAR_UUID,
    BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY
  );
  notifyCharacteristic->addDescriptor(new BLE2902());

  BLECharacteristic* writeCharacteristic = service->createCharacteristic(
    WRITE_CHAR_UUID,
    BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR
  );
  writeCharacteristic->setCallbacks(new SmartShieldWriteCallbacks());

  service->start();

  BLEAdvertising* advertising = BLEDevice::getAdvertising();
  advertising->addServiceUUID(SERVICE_UUID);
  advertising->setScanResponse(true);
  advertising->setMinPreferred(0x06);
  advertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();

  Serial.print("[BLE] Advertising started as ");
  Serial.println(BLE_DEVICE_NAME);
  Serial.print("[BLE] Local MTU set to ");
  Serial.println(BLE_MTU_SIZE);
}
