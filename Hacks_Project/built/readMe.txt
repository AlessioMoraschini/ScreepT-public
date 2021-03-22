### Anti-ScreenLocker tool ###

java -cp hacks.jar antilocker.AntilockerGui


### Keylogger ###

#V1 - with optional parameter destination folder with infinite timeout
java -cp hacks.jar keylogger.keyLogTest "C:\Users\yourAccount\Desktop\myDestination"

#V2 - with optional parameters destination folder and seconds to wait before close
java -cp hacks.jar keylogger.keyLogTest "C:\Users\yourAccount\Desktop\myDestination" 60

#V3 - creating default folder "C:\Users\yourAccount\Desktop\LOGGER" with infinite timeout
java -cp hacks.jar keylogger.keyLogTest 


### bonus love function drawer ###
java -cp hacks.jar function.AbstractCurveFunction 
