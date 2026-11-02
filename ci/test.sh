#!/bin/bash
# Corre no emulador do GitHub Actions: instala, abre e regista erros/capturas.
R=/tmp/report
mkdir -p $R
P=com.carlos.liquid
adb wait-for-device
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb install -r Liquid.apk > $R/install.txt 2>&1
shot() { adb exec-out screencap -p > $R/$1.png; }
logs() { adb logcat -d -v time > $R/logcat_$1_full.txt; grep -E "AndroidRuntime|FATAL|F DEBUG|F libc|Abort|SIGSEGV|SIGABRT|$P|hwui|RuntimeShader|Exception|Caused by|at com\.carlos" $R/logcat_$1_full.txt | grep -v "I/ActivityTaskManager: Displayed" | head -400 > $R/logcat_$1.txt; }

adb logcat -c
# 1) arranque limpo
adb shell am start -W -n $P/.MainActivity > $R/start1.txt 2>&1
sleep 15
shot home1
logs 1
adb shell dumpsys activity exit-info $P | head -80 > $R/exitinfo1.txt

# 2) com permissões e como launcher predefinido
adb logcat -c
adb shell pm grant $P android.permission.READ_CALENDAR
adb shell pm grant $P android.permission.BLUETOOTH_CONNECT
adb shell appops set $P SYSTEM_ALERT_WINDOW allow
adb shell cmd notification allow_listener $P/$P.lock.NotifService
adb shell cmd role add-role-holder --user 0 android.app.role.HOME $P 0 > $R/role.txt 2>&1
adb shell am force-stop $P
adb shell input keyevent 3
sleep 15
shot home2
adb shell input swipe 1000 1200 150 1200 300
sleep 4
shot page2
adb shell input swipe 150 1200 1000 1200 300
sleep 2
adb shell input swipe 150 1200 1000 1200 300
sleep 4
shot today
adb shell input keyevent 3
sleep 3
adb shell am start -n $P/.SettingsActivity
sleep 6
shot settings
logs 2
adb shell dumpsys activity exit-info $P | head -80 > $R/exitinfo2.txt
