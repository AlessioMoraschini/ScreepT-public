set JDK_PATH=C:\Program Files\java/jdk1.8.0_162/bin; 

set PATH=%JDK_PATH%%PATH% 

echo %PATH% 

java -jar updater.jar -zipFolders-excludeParent-generateHashes false manifest.properties
