"""Capture the built application on the disposable CI emulator after tests."""
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

output = Path("ui-screenshots")
output.mkdir(exist_ok=True)

def adb(*args, binary=False):
    return subprocess.check_output(["adb", *args], text=not binary)

def wait_for(description):
    for _ in range(12):
        try:
            adb("shell", "uiautomator", "dump", "/sdcard/navglas-window.xml")
            root = ET.fromstring(adb("shell", "cat", "/sdcard/navglas-window.xml"))
            for node in root.iter("node"):
                if node.get("content-desc") == description and node.get("enabled") == "true":
                    return node
        except (subprocess.CalledProcessError, ET.ParseError):
            pass
        time.sleep(1)
    raise RuntimeError(f"Control did not become ready: {description}")

def screenshot(name):
    (output / name).write_bytes(adb("exec-out", "screencap", "-p", binary=True))

adb("install", "-r", "app/build/outputs/apk/debug/app-debug.apk")
for permission in ("RECORD_AUDIO", "READ_CONTACTS", "POST_NOTIFICATIONS"):
    adb("shell", "pm", "grant", "com.example.naviglas", f"android.permission.{permission}")
adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
adb("shell", "wm", "dismiss-keyguard")
adb("shell", "settings", "put", "system", "accelerometer_rotation", "0")
adb("shell", "settings", "put", "system", "user_rotation", "0")
adb("shell", "am", "start", "-W", "-n", "com.example.naviglas/.MainActivity")
wait_for("Pokreni slušanje")
screenshot("home-portrait.png")
settings = wait_for("Podešavanja")
coordinates = list(map(int, re.findall(r"\d+", settings.get("bounds", ""))))
if len(coordinates) != 4:
    raise RuntimeError("Settings control has no usable bounds")
adb("shell", "input", "tap", str((coordinates[0] + coordinates[2]) // 2), str((coordinates[1] + coordinates[3]) // 2))
time.sleep(1)
screenshot("settings.png")
adb("shell", "input", "keyevent", "KEYCODE_BACK")
adb("shell", "settings", "put", "system", "user_rotation", "1")
time.sleep(2)
wait_for("Pokreni slušanje")
screenshot("home-landscape.png")
print("Captured portrait, settings and landscape screens")

