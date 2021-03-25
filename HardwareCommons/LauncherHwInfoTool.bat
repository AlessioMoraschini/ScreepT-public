set JDK_PATH=C:\Program Files\java/jdk1.8.0_162/bin; 

set PATH=%JDK_PATH%%PATH% 

echo %PATH% 

java -Xmx256m -Xms128m -Xss56m -XX:MaxPermSize=38m -XshowSettings:vm -Dfile.encoding=UTF8 -Dawt.useSystemAAFontSettings=on -Dsun.java2d.d3d=false -Dswing.metalTheme=steel -Dsun.java2d.ddscale=true -Dsun.java2d.uiScale=1 -Djava.library.path="./Resources_ScreepT/lib" -jar HardwareInfoTool.jar.jar

exit 0