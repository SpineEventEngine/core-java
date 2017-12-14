::
:: A script for updating Spine configuration.
::
:: The script downloads the generic script, which updates the specified files (see below).
::
:: To use the script, copy it to a sub folder of your project
:: (e.g. to .../IdeaProjects/project-name/config) and execute it.

:: The URL of the generic script. See details by this URL.
set UPDATE_SCRIPT_URL="https://raw.githubusercontent.com/SpineEventEngine/core-java/master/config/update-files.bat"

set LOCAL_SCRIPT_NAME="update.bat"

:: Download the script.
bitsadmin.exe /transfer "Download" %UPDATE_SCRIPT_URL% "%LOCAL_SCRIPT_NAME%"
if %ERRORLEVEL% neq 0 (
    echo "Cannot download the script."
)

:: Update the specified files from the `core-java`.
::
:: You can change the list of files in a project, which uses the script, according to your needs.
:: But the version in `core-java` should specify the list files,
:: that should be updated in a default scenario.
"%LOCAL_SCRIPT_NAME%" ^
    ^
    .idea/copyright/profiles_settings.xml ^
    .idea/copyright/TeamDev_Open_Source.xml ^
    .idea/inspectionProfiles/Project_Default.xml ^
    .idea/codeStyleSettings.xml ^
    ^
    scripts/generate-descriptor-set.gradle ^
    scripts/jacoco.gradle ^
    scripts/no-internal-javadoc.gradle ^
    scripts/publish.sh ^
    scripts/report-coverage.sh ^
    scripts/test-artifacts.gradle ^
    ^
    .gitignore

:: Remove the downloaded script.
del "%LOCAL_SCRIPT_NAME%"
