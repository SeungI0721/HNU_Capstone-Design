/* Smart Shield 유틸리티 모듈: 센서값 검증과 기본값 처리를 담당합니다. */
#pragma once

bool isInRange(float value, float minValue, float maxValue) {
  return !isnan(value) && value >= minValue && value <= maxValue;
}

String formatFloatOrEmpty(float value, float minValue, float maxValue, uint8_t decimals) {
  if (!isInRange(value, minValue, maxValue)) {
    return "";
  }
  return String(value, (unsigned int)decimals);
}

String formatIntOrEmpty(float value, int minValue, int maxValue) {
  if (!isInRange(value, minValue, maxValue)) {
    return "";
  }
  return String((int)round(value));
}

String valueOrFallback(const String& value, const char* fallbackValue) {
  if (value.length() > 0) {
    return value;
  }
  return USE_APP_SAFE_FALLBACK_VALUES ? String(fallbackValue) : String("");
}
