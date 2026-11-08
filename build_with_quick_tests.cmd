@echo off
rem Quick tests first, then the complete build (Java 8 and Maven 3.8.x in the PATH). Stops if a test fails.
setlocal
call "%~dp0run_tests.cmd" quick
if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%
set MAVEN_OPTS=-Dfile.encoding=UTF-8
pushd "%~dp0ScreepT_Parent"
call mvn -o clean install -DskipTests -Dskip=true -Dproject.build.sourceEncoding=UTF-8
set RESULT=%ERRORLEVEL%
popd
exit /b %RESULT%
