# MySysScan3
- Small system analysis app for Android devices to compile ON Android devices -
Written without Android Studio/Eclipse with command line editors for compilation on Android devices using the TERMUX environment.

# Compile and install on phone/tablet

- install TERMUX from Play Store or F-Droid
- from TERMUX commandline install build-tools: dx, aapt, apksigner
- from TERMUX commandline install ecj (Java compiler)

Then you compile the whole bunch:

- copy project to a folder on your phone/tablet accessible by TERMUX
- copy the android.jar file of Android SDK 23 (in SDK-folder platforms/android-23) to a folder accessible by TERMUX
- start TERMUX
- adjust the SDK variable in the build.sh file to point to the folder where you've put android.jar
- start build.sh script in the root of the app directory
- select the FIRST!!! option in the menu to generate an APK file
- wait a while...
- find and install the resulting APK in the bin dir of the project

# Compile and install on Linux PC or Raspberry Pi

- get a copy of the Android build tools (I've used contents of subfolder build-tools/26.0.1 of Andoid SDK)
- make an entry for your development machine in the "case $HOSTNAME in..." statement in line 46 of the script
- copy the android.jar file of Android SDK 23 (in SDK-folder platforms/android-23) to a folder accessible
- adjust the SDK variable in the build.sh file to point to the folder where you've put android.jar
- adjust the BUILDTOOLSPATH variables in the build.sh file to point to the build tools
- start build.sh script in the root of the app directory
- select an option in the menu (for options 2,3 connect a device to your USB to install and run directly on device)
- wait and see

