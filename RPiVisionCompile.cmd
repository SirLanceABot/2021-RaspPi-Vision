echo on
rem Access this file through VS Code: ctrl shift P, Tasks: Run Task, Build RPi Vision
pushd "C:\Users\Public\wpilib\2021\frccode"
call frcvars2021.bat
popd
rem echo on
rem set DEBUG=Y
call gradlew.bat build
echo off
echo 1) On the rPi web dashboard: Make the rPi writable by selecting the "Writable" button

echo 2) In the rPi web dashboard Application tab: Select the "Uploaded Java jar" option for Application

echo 3) Click "Browse..." and select the "name_of_your_project_root_folder-all.jar" file in
echo    your project root directory in the build/libs subdirectory

echo 4) Click Save

echo The application will be started automatically.  Console output can be seen by
echo enabling console output in the Vision Status tab.
rem pause
exit