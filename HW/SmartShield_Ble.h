/* Smart Shield BLE 모듈: 광고, Notify, Write 명령 처리를 담당합니다. */
#pragma once

void handleBleCommand(String command) {
  command.trim();
  Serial.print("[BLE WRITE] Received command: ");
  Serial.println(command);

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
    bleConnected = true;
    Serial.println("[BLE] Android app connected");
    Serial.println("[BLE] Android must enable notification on FFF1 CCCD 0x2902");
  }

  void onDisconnect(BLEServer* server) override {
    bleConnected = false;
    restartAdvertising = true;
    Serial.println("[BLE] Disconnected, advertising will restart");
  }
};

class SmartShieldWriteCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* characteristic) override {
    String command = String(characteristic->getValue().c_str());
    handleBleCommand(command);
  }
};

void initBle() {
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
