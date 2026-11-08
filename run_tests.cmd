@echo off
rem ScreepT automatic tests, three tiers (see TESTING.md):
rem   run_tests.cmd quick    ~ seconds, pure logic        -> every build
rem   run_tests.cmd medium   ~ half a minute, real files  -> daily
rem   run_tests.cmd full     ~ a minute, opens windows    -> weekly / before a release
rem Needs JDK 8 and Maven 3.8.x in the PATH.
setlocal
set TIER=%1
if "%TIER%"=="" set TIER=quick
if /I "%TIER%"=="quick"  set PATTERN=*QuickTest
if /I "%TIER%"=="medium" set PATTERN=*QuickTest,*MediumTest
if /I "%TIER%"=="full"   set PATTERN=*QuickTest,*MediumTest,*FullTest
if "%PATTERN%"=="" (
  echo Unknown tier "%TIER%": use quick, medium or full
  exit /b 2
)
set MAVEN_OPTS=-Dfile.encoding=UTF-8
pushd "%~dp0ScreepT_Parent"
echo Running the %TIER% tests (%PATTERN%)...
call mvn -o test -Dtest=%PATTERN% -DfailIfNoTests=false -Dproject.build.sourceEncoding=UTF-8
set RESULT=%ERRORLEVEL%
popd
if %RESULT% neq 0 (echo TESTS FAILED: %TIER% & exit /b %RESULT%)
echo Tests OK: %TIER%
endlocal
