
start javaw -jar "Puppynoid.jar"
IF ERRORLEVEL 1 GOTO ERROR_HANDLING_1
ECHO Launched with javaw :)
EXIT

:ERROR_HANDLING_1
ECHO Javaw not found. Retrying with java command...
java -jar "Puppynoid.jar"
ECHO Launched with java :)
IF ERRORLEVEL 1 GOTO ERROR_HANDLING_2
EXIT

:ERROR_HANDLING_2
ECHO "Java command not found. Please install java (version >= 1.8) and retry."
PAUSE
EXIT