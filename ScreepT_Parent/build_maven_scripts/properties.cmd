set "screept_parent_dir=%~dp0.."
set "plugins_parent_dir=%~dp0..\..\PluginsParent"
set "global_parent_dir=%~dp0..\..\CommonParentGlobal"

echo "screept_parent_dir: %screept_parent_dir%"
echo "plugins_parent_dir: %plugins_parent_dir%"
echo "global_parent_dir:  %global_parent_dir%"

mode con: col=180 lines=20000

:: Build properties
set "FLAG_THREADS=-T 1"
set "FLAG_UPDATE=-U"
set "FLAG_OFFLINE="
set "FLAG_EMBEDDED_PLUGINS=-P default_with_embedded_plugin"
set "FLAG_SCREEPT_PARENT_PLUGINS=-f pomWithPlugins.xml"

set "MAVEN_COMMAND_BASE=mvn %FLAG_THREADS% clean install %FLAG_UPDATE% %FLAG_OFFLINE%"
set "MAVEN_BUILD_STANDARD=%MAVEN_COMMAND_BASE%"
set "MAVEN_BUILD_SCREEPT_EMBEDDED_PLUGINS=%MAVEN_COMMAND_BASE% %FLAG_EMBEDDED_PLUGINS%"
set "MAVEN_BUILD_SCREEPT_EXTERNAL_PLUGINS=%MAVEN_COMMAND_BASE% %FLAG_SCREEPT_PARENT_PLUGINS%"
set "MAVEN_BUILD_SCREEPT_ALL_PLUGINS=%MAVEN_COMMAND_BASE% %FLAG_EMBEDDED_PLUGINS% %FLAG_SCREEPT_PARENT_PLUGINS%"

echo "%MAVEN_BUILD_LAUNCH%"

cmd /k "mvn -version && exit 0"
