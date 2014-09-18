
set seed=%random%
start mylogcat.bat
start monkey_time.bat

adb -d shell monkey -p com.baidu.wearable --throttle 500 --pct-anyevent 0 --pct-trackball 0 --pct-majornav 30 --ignore-crashes --ignore-timeouts --ignore-security-exceptions  --monitor-native-crashes -v -v -v -s %seed% 7500000 > nul

//adb shell "killall -9 logcat"

java TransTxtToHtml monkey_log.txt com.baidu.wearable

@ping 127.0.0.1 -n 180 > nul
adb -s %devices% shell input keyevent 82