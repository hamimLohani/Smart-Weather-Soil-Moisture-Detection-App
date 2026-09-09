#!/bin/bash
# =============================================================================
# AgroSense Node — Flash Script
# Compiles and uploads AgroSenseNode.ino to an ESP8266 NodeMCU using
# arduino-cli. Automatically detects the serial port, installs the ESP8266
# board package and all required libraries if missing.
#
# Usage:
#   bash flash.sh              # Auto-detect port, compile + upload
#   bash flash.sh /dev/cu.xxx  # Specify port explicitly
#   bash flash.sh --monitor    # Flash then open Serial Monitor
#   bash flash.sh --libs-only  # Only install libraries, don't flash
# =============================================================================

set -e

# --- Config -------------------------------------------------------------------
SKETCH="firmware/AgroSenseNode/AgroSenseNode.ino"
BOARD="esp8266:esp8266:nodemcuv2"
BAUD=115200
ESP_PLATFORM="esp8266:esp8266"
ESP_BOARD_URL="https://arduino.esp8266.com/stable/package_esp8266com_index.json"

BOLD='\033[1m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

# Parse arguments
PORT_ARG=""
OPEN_MONITOR=false
LIBS_ONLY=false

for arg in "$@"; do
  case "$arg" in
    --monitor)    OPEN_MONITOR=true ;;
    --libs-only)  LIBS_ONLY=true ;;
    /dev/*)       PORT_ARG="$arg" ;;
    COM*)         PORT_ARG="$arg" ;;
    *)            echo -e "${RED}Unknown argument: $arg${NC}"; exit 1 ;;
  esac
done

echo ""
echo -e "${BOLD}${CYAN}============================================================${NC}"
echo -e "${BOLD}${CYAN}   AgroSense Node — ESP8266 Flash Script${NC}"
echo -e "${BOLD}${CYAN}============================================================${NC}"
echo ""

# --- Step 0: Check arduino-cli is installed ----------------------------------
echo -e "${BOLD}[1/5] Checking arduino-cli...${NC}"
if ! command -v arduino-cli &> /dev/null; then
  echo -e "${YELLOW}arduino-cli not found. Installing via Homebrew...${NC}"
  if command -v brew &> /dev/null; then
    brew install arduino-cli
  else
    echo -e "${RED}Homebrew not found. Install arduino-cli manually:${NC}"
    echo "  https://arduino.github.io/arduino-cli/latest/installation/"
    exit 1
  fi
fi
echo -e "  ${GREEN}✓${NC} arduino-cli $(arduino-cli version | head -1)"

# --- Step 1: Add ESP8266 board URL and update index --------------------------
echo ""
echo -e "${BOLD}[2/5] Configuring ESP8266 board package...${NC}"

# Add the board URL to arduino-cli config if not already there
CURRENT_URLS=$(arduino-cli config get board_manager.additional_urls 2>/dev/null || echo "")
if [[ "$CURRENT_URLS" != *"esp8266"* ]]; then
  echo "  Adding ESP8266 board manager URL..."
  arduino-cli config add board_manager.additional_urls "$ESP_BOARD_URL"
fi

# Update index
arduino-cli core update-index --additional-urls "$ESP_BOARD_URL" > /dev/null
echo -e "  ${GREEN}✓${NC} Board index updated."

# Install ESP8266 core if missing
if ! arduino-cli core list | grep -q "$ESP_PLATFORM"; then
  echo -e "  Installing ESP8266 core (this may take a few minutes)..."
  arduino-cli core install "$ESP_PLATFORM" --additional-urls "$ESP_BOARD_URL"
  echo -e "  ${GREEN}✓${NC} ESP8266 core installed."
else
  echo -e "  ${GREEN}✓${NC} ESP8266 core already installed."
fi

# --- Step 2: Install required libraries ---------------------------------------
echo ""
echo -e "${BOLD}[3/5] Installing required libraries...${NC}"

install_lib_if_missing() {
  local lib_name="$1"
  if ! arduino-cli lib list | grep -q "$lib_name"; then
    echo "  Installing: $lib_name"
    arduino-cli lib install "$lib_name"
    echo -e "  ${GREEN}✓${NC} $lib_name installed."
  else
    echo -e "  ${GREEN}✓${NC} $lib_name already installed."
  fi
}

install_lib_if_missing "DHT sensor library"
install_lib_if_missing "Adafruit Unified Sensor"
install_lib_if_missing "ArduinoJson"

if [ "$LIBS_ONLY" = true ]; then
  echo ""
  echo -e "${GREEN}Libraries installed. Exiting (--libs-only mode).${NC}"
  exit 0
fi

# --- Step 3: Auto-detect serial port ------------------------------------------
echo ""
echo -e "${BOLD}[4/5] Detecting serial port...${NC}"

if [ -n "$PORT_ARG" ]; then
  PORT="$PORT_ARG"
  echo -e "  Using specified port: ${CYAN}$PORT${NC}"
else
  # Auto-detect NodeMCU/CP2102 or CH340 serial ports on macOS
  PORT=$(ls /dev/cu.* 2>/dev/null | grep -E "(usbserial|SLAB|CH340|wchusbserial)" | head -1)

  if [ -z "$PORT" ]; then
    echo -e "${YELLOW}  No ESP8266 port auto-detected. Available ports:${NC}"
    ls /dev/cu.* 2>/dev/null | sed 's/^/    /'
    echo ""
    echo -e "${YELLOW}  Connect your NodeMCU and re-run, or specify port:${NC}"
    echo -e "  ${CYAN}bash flash.sh /dev/cu.your-port-name${NC}"
    exit 1
  fi

  echo -e "  Auto-detected: ${CYAN}$PORT${NC}"
fi

# Verify port exists
if [ ! -e "$PORT" ]; then
  echo -e "${RED}Error: Port $PORT not found. Is the NodeMCU plugged in?${NC}"
  exit 1
fi

# --- Step 4: Compile and Upload -----------------------------------------------
echo ""
echo -e "${BOLD}[5/5] Compiling and uploading firmware...${NC}"
echo -e "  Sketch:  ${CYAN}$SKETCH${NC}"
echo -e "  Board:   ${CYAN}$BOARD${NC}"
echo -e "  Port:    ${CYAN}$PORT${NC}"
echo -e "  Baud:    ${CYAN}$BAUD${NC}"
echo ""

arduino-cli compile \
  --fqbn "$BOARD" \
  "$SKETCH"

echo ""
echo -e "  ${GREEN}✓${NC} Compilation successful. Uploading..."
echo ""

arduino-cli upload \
  --fqbn "$BOARD" \
  --port "$PORT" \
  "$SKETCH"

echo ""
echo -e "${GREEN}============================================================${NC}"
echo -e "${GREEN}  ✓ Firmware uploaded successfully!${NC}"
echo -e "${GREEN}============================================================${NC}"
echo ""
echo -e "  Device will now boot and broadcast a WiFi hotspot."
echo -e "  Look for a network named: ${CYAN}AgroSense-XXXX-XXXX${NC}"
echo -e "  The ${BOLD}XXXX-XXXX${NC} suffix IS your pairing code."
echo -e "  Connect → open ${CYAN}http://192.168.4.1${NC} → enter WiFi credentials."
echo ""

# --- Optional: Open Serial Monitor --------------------------------------------
if [ "$OPEN_MONITOR" = true ]; then
  echo -e "${BOLD}Opening Serial Monitor at ${BAUD} baud (Ctrl+C to exit)...${NC}"
  echo ""
  sleep 2  # Give the device time to reboot
  arduino-cli monitor \
    --port "$PORT" \
    --config baudrate="$BAUD"
fi
