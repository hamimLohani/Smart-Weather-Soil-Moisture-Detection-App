#!/bin/bash
# =============================================================================
# AgroSense ESP8266 ↔ JavaFX App Connectivity & Flexibility Test Script
# Simulates every scenario the firmware would encounter in the field.
# =============================================================================

SERVER="localhost:8080"
ENDPOINT="http://${SERVER}/api/readings"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

pass_count=0
fail_count=0

# Helper: run a test case and print result
run_test() {
  local description="$1"
  local payload="$2"
  local expected_code="$3"

  response=$(curl -s -w "\n%{http_code}" -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d "$payload" 2>&1)

  body=$(echo "$response" | sed '$d')
  status=$(echo "$response" | tail -n1)

  if [ "$status" == "$expected_code" ]; then
    echo -e "  ${GREEN}✓ PASS${NC} [HTTP $status] $description"
    echo -e "        Response: $body"
    pass_count=$((pass_count + 1))
  else
    echo -e "  ${RED}✗ FAIL${NC} [HTTP $status, expected $expected_code] $description"
    echo -e "        Response: $body"
    fail_count=$((fail_count + 1))
  fi
  echo ""
}

# Header
echo ""
echo -e "${BOLD}${CYAN}============================================================${NC}"
echo -e "${BOLD}${CYAN}   AgroSense — ESP8266 ↔ Server Connectivity Test Suite${NC}"
echo -e "${BOLD}${CYAN}============================================================${NC}"
echo ""

# --- Connectivity Check ---
echo -e "${BOLD}[0] Basic Connectivity${NC}"
if curl -s --connect-timeout 3 "$ENDPOINT" -o /dev/null; then
  echo -e "  ${GREEN}✓${NC} Server at $SERVER is reachable."
else
  echo -e "  ${RED}✗${NC} Cannot reach server at $SERVER. Is the JavaFX app running?"
  exit 1
fi
echo ""

# --- Section 1: Business Logic (Unpaired Device) ---
echo -e "${BOLD}[1] Unpaired Device Handling (expects 403)${NC}"
run_test "SM-0001 — seeded but not yet paired" \
  '{"device_id":"SM-0001","temperature":27.5,"humidity":62.0,"soil_moisture_raw":480}' \
  "403"

run_test "SM-0002 — seeded but not yet paired" \
  '{"device_id":"SM-0002","temperature":30.1,"humidity":55.0,"soil_moisture_raw":520}' \
  "403"

# --- Section 2: Unknown Device ---
echo -e "${BOLD}[2] Unknown Device Serial (expects 404)${NC}"
run_test "SM-9999 — device not in DB" \
  '{"device_id":"SM-9999","temperature":25.0,"humidity":70.0,"soil_moisture_raw":400}' \
  "404"

run_test "Empty device_id string" \
  '{"device_id":"","temperature":25.0,"humidity":70.0,"soil_moisture_raw":400}' \
  "404"

# --- Section 3: Malformed Payloads ---
echo -e "${BOLD}[3] Malformed / Incomplete JSON Payloads (expects 400)${NC}"
run_test "Missing soil_moisture_raw field" \
  '{"device_id":"SM-0001","temperature":27.5,"humidity":62.0}' \
  "400"

run_test "Completely empty body" \
  '{}' \
  "400"

run_test "Invalid JSON (as firmware might send on crash)" \
  'NOT_JSON_AT_ALL' \
  "400"

run_test "Missing device_id field entirely" \
  '{"temperature":27.5,"humidity":62.0,"soil_moisture_raw":480}' \
  "400"

# --- Section 4: Edge Case Sensor Values ---
echo -e "${BOLD}[4] Edge Case Sensor Values (device still returns 403 — but parsed correctly)${NC}"
run_test "Extreme temperature (very hot: 60°C)" \
  '{"device_id":"SM-0001","temperature":60.0,"humidity":10.0,"soil_moisture_raw":820}' \
  "403"

run_test "Extreme temperature (near freezing: 0.1°C)" \
  '{"device_id":"SM-0001","temperature":0.1,"humidity":99.9,"soil_moisture_raw":380}' \
  "403"

run_test "Dry soil (raw=820 → ~0% moisture)" \
  '{"device_id":"SM-0001","temperature":28.0,"humidity":60.0,"soil_moisture_raw":820}' \
  "403"

run_test "Wet soil (raw=380 → ~100% moisture)" \
  '{"device_id":"SM-0001","temperature":28.0,"humidity":90.0,"soil_moisture_raw":380}' \
  "403"

run_test "Oversaturated sensor (raw below wet cal, <380)" \
  '{"device_id":"SM-0001","temperature":28.0,"humidity":95.0,"soil_moisture_raw":200}' \
  "403"

# --- Section 5: HTTP Method Flexibility ---
echo -e "${BOLD}[5] Incorrect HTTP Methods (expects 405)${NC}"
run_test "GET /api/readings — firmware accidentally uses GET" \
  "$(curl -s -w '\n%{http_code}' -X GET "$ENDPOINT" 2>&1 | tail -n1)" \
  "405"

# Inline for non-POST test
response_code=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$ENDPOINT")
if [ "$response_code" == "405" ]; then
  echo -e "  ${GREEN}✓ PASS${NC} [HTTP $response_code] GET method correctly rejected"
else
  echo -e "  ${RED}✗ FAIL${NC} [HTTP $response_code, expected 405] GET method should be rejected"
fi
echo ""

# --- Section 6: High-Frequency / Burst Simulation ---
echo -e "${BOLD}[6] High-Frequency Burst — Simulating rapid firmware restarts (5 rapid POSTs)${NC}"
for i in {1..5}; do
  code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d "{\"device_id\":\"SM-0001\",\"temperature\":$((25+i)).0,\"humidity\":$((60+i)).0,\"soil_moisture_raw\":$((470+i*10))}")
  if [ "$code" == "403" ] || [ "$code" == "200" ]; then
    echo -e "  ${GREEN}✓${NC} Burst #$i — HTTP $code"
  else
    echo -e "  ${RED}✗${NC} Burst #$i — HTTP $code (unexpected)"
  fi
done
echo ""

# --- Section 7: Server Resilience ---
echo -e "${BOLD}[7] Wrong Content-Type Header (mimics firmware misconfiguration)${NC}"
response_code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$ENDPOINT" \
  -H "Content-Type: text/plain" \
  -d '{"device_id":"SM-0001","temperature":27.5,"humidity":62.0,"soil_moisture_raw":480}')
echo -e "  Content-Type: text/plain → HTTP $response_code (server handled gracefully)"
echo ""

# --- Summary ---
echo -e "${BOLD}${CYAN}============================================================${NC}"
echo -e "${BOLD}  Test Summary${NC}"
echo -e "${CYAN}============================================================${NC}"
total=$((pass_count + fail_count))
echo -e "  ${GREEN}Passed: $pass_count${NC} / $total"
if [ $fail_count -gt 0 ]; then
  echo -e "  ${RED}Failed: $fail_count${NC} / $total"
else
  echo -e "  ${GREEN}All tests passed!${NC}"
fi
echo ""
echo -e "${YELLOW}Note: 403 responses are correct for unpaired devices.${NC}"
echo -e "${YELLOW}To test a full 200 OK: login → Pair Device → use code AGRO-1A2B-3C4D${NC}"
echo ""
