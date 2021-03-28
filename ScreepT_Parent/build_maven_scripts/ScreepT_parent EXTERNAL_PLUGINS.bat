set "batdir=%~dp0"
cd /D "%batdir%"
echo "Current execution dir: %CD%"

call properties.cmd

:: move to parent directory, and launch maven build for Common parent
cd /D "%global_parent_dir%"
cmd /k "%MAVEN_BUILD_STANDARD% && exit 0"

:: move to parent directory, and launch maven build for ScreepT_Parent
cd /D "%screept_parent_dir%"
cmd /k "%MAVEN_BUILD_SCREEPT_EXTERNAL_PLUGINS% && exit 0"



if %ERRORLEVEL% GEQ 1 goto error

echo "Build finished :)"
pause
exit 0

:error
start cmd /k "cd && echo. && echo Cannot launch build, an error occurred. && echo. && pause && exit 0"
exit 1