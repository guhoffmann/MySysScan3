#!/bin/bash

################## build.sh (C) GU Hoffmann 05.06.2018 ###################
#
# Update the basic Android App with commandline tools on a X86-PC,
# Raspberry Pi or an Android device running TERMUX!
#                        COMPLETE-VERSION
#
##########################################################################2

SCRIPTNAME="buildapp 18.06.18"

WHITE="\033[0;37m"
GREEN="\033[1;32m"
BLUE="\033[1;34m"

clear

echo -e $GREEN"*** $SCRIPTNAME ***\n"$WHITE

if [ $(uname -o) = "Android" ];then
   HOSTNAME="Android"
   # TERMUX: See if grep comes from busybox or GNU!
   # For GNU grep set --color marking on
   if [ -f $PREFIX/bin/grep ];then
      GREP='$PREFIX/bin/grep --color'
   else
      GREP="grep"
   fi
else
   HOSTNAME=$(cat /etc/hostname)
   # highlight some things with GNU grep!
   GREP="grep --color"
fi

echo -e $BLUE"\nHostname: $HOSTNAME\n"$WHITE

##########################################################################
######################### Prerequisites ##################################
#################### Determine paths and tools ###########################
##########################################################################

# Select SDK 23 = Android 6.0
# Necessary for using Camera2-Api!!!

PLATFORM="android-23-6.0"

# Names of app,package and apk to create

APPNAME="MySysScan3"
APKNAME="MySysScan3"

PACKAGEPATH="com/uweandapp"
PACKAGENAME="com.uweandapp"

# Get locations of project files and sparSDK with android.jar files
# from SDK 19(4.4 KitKat) and SDK 23(6.0 Marshmallow) only!
# Build-Tools in sparSDK are 26.0.1 for now on PC/Notebook!
# These are used for X86/AMD64 PC's only.
# On Android devices/Pi we use the google tools ARM/TERMUX!

case $HOSTNAME in

   B590)
      SDK=/home/uwe/MyDevelop/MyAndroid/sparSDK/$PLATFORM/android.jar
      # BUILDTOOLSPATH needs "/" at the end to get subsequent script working
      BUILDTOOLSPATH=/media/uwe/backup/data/Android/Sdk/build-tools/26.0.1/
      PROJECTDIR=/home/uwe/MyDevelop/MyAndroid/$APPNAME
      # Deklaration of build-tools 
      AAPT=$BUILDTOOLSPATH""aapt
      APKSIGNER=jarsigner
      ZIPALIGN=$BUILDTOOLSPATH""zipalign
      DX=dalvik-exchange
      JAVAC="ecj -source 1.7 -target 1.7 "
      ;;
      
   senior-medion)
      SDK=/home/uwe/MyDevelop/MyAndroid/sparSDK/$PLATFORM/android.jar
      # BUILDTOOLSPATH needs "/" at the end to get subsequent script working
      BUILDTOOLSPATH=/home/uwe/Android/Sdk/build-tools/26.0.1/
      PROJECTDIR=/home/uwe/MyDevelop/MyAndroid/$APPNAME
      # Deklaration of build-tools 
      AAPT=$BUILDTOOLSPATH""aapt
      APKSIGNER=jarsigner
      ZIPALIGN=$BUILDTOOLSPATH""zipalign
      DX=dalvik-exchange
      JAVAC="ecj -source 1.7 -target 1.7 "
      ;;

   gamepi)
      SDK=/media/pi/kingston-8g-ext4/sparSDK/$PLATFORM/android.jar
      # BUILDTOOLSPATH needs "/" at the end to get subsequent script working
      BUILDTOOLSPATH=""
      PROJECTDIR=/media/pi/kingston-8g-ext4/MyDevelop/MyAndroid/$APPNAME
      # Deklaration of build-tools 
      AAPT=$BUILDTOOLSPATH""aapt
      APKSIGNER=jarsigner
      ZIPALIGN=$BUILDTOOLSPATH""zipalign
      DX=dalvik-exchange
      JAVAC="ecj -source 1.7 -target 1.7 "
      ;;

   Android)
      SDK=~/uwe/sparSDK/$PLATFORM/android.jar
      # BUILDTOOLSPATH needs "/" at the end to get subsequent script working
      BUILDTOOLSPATH=""
      PROJECTDIR=~/uwe/$APPNAME
      # Deklaration of build-tools 
      AAPT=$BUILDTOOLSPATH""aapt
      APKSIGNER=apksigner
      ZIPALIGN=$BUILDTOOLSPATH""zipalign
      DX=dx
      JAVAC="ecj -source 1.7 -target 1.7 "
      ;;
      
   *)
      exit;;
esac

##########################################################################
######################### Prerequisites ##################################
#########################  "Functions"  ##################################
##########################################################################

# Clean project

CLEANFUNC="
           rm $PROJECTDIR/bin/*.*; \
           rm -r $PROJECTDIR/obj/*.*; \
           rm $PROJECTDIR/src/$PACKAGEPATH/$APPNAME/R.java; \
           find -L $PROJECTDIR -name \"classes.dex\" -delete \
           "

# Now go on and select what to do...

MENU="$APPNAME-New \
      $APPNAME-New_with_test_on_device \
      $APPNAME-New_with_test_on_device_with_LogCat \
      $APPNAME-Run-without-compile \
      $APPNAME-Clean \
      Abbruch"
      
PS3="Auswahl:"
COLUMNS=1

select ITEM in $MENU
do
   case $ITEM in
   
      $APPNAME-New)
         bash -c "$CLEANFUNC"
         break;;
         
      $APPNAME-New_with_test_on_device)
         bash -c "$CLEANFUNC"
         break;;

      $APPNAME-New_with_test_on_device_with_LogCat)
         bash -c "$CLEANFUNC"
         break;;
         
      $APPNAME-Run-without-compile)
         break;;
         
      $APPNAME-Clean)
         bash -c "$CLEANFUNC"
         echo "Project cleaned!"
         exit;;
         
      Abbruch)
         echo "Break!"
         exit;;
         
      *)
         echo "Break!"
         exit;;
   esac
done

clear

##########################################################################
######################### Prerequisites ##################################
####### Specify compiler options to get only useful messages #############
##########################################################################

#COPTIONS="-warn:-allDeprecation"

##########################################################################
###################### Compi2le and create app ############################
##########################################################################

if [ $ITEM != "$APPNAME-Run-without-compile" ]\
   && [ $ITEM != "$APPNAME-Clean" ]; then

   STARTZEIT=$(date +%s)

   # compile all resources in projects res dir to R.java
   # for accessing ressources from Java source code

   echo -e $GREEN"\n=> Creating R.java..."$WHITE
   $AAPT package -f -m -J $PROJECTDIR/src -M $PROJECTDIR/AndroidManifest.xml \
         -S $PROJECTDIR/res -I $SDK

   # Compile Java files

   echo -e $GREEN"\n=> Compiling java..."$WHITE
   $JAVAC $COPTIONS -d $PROJECTDIR/obj -classpath $PROJECTDIR/src: -bootclasspath $SDK \
        $PROJECTDIR/src/*.java 2>&1|$GREP -E '^|WARNING|ERROR'


   COMPILEZEIT=$(date +%s)

   # Make a dex file

   echo -e $GREEN"\n=> Making Dex..."$WHITE
   $DX --dex --output=$PROJECTDIR/bin/classes.dex $PROJECTDIR/obj

   #If you have the error UNEXPECTED TOP-LEVEL EXCEPTION, it can be because
   # you use old build tools and DX try to translate java 1.7 rather than 1.8.
   # To solve the problem, you have to specify 1.7 java version in the previous
   #javac command:
   #javac -d obj -source 1.7 -target 1.7 ...

   # Put everything in an APK

   echo -e $GREEN"\n=> Making unsigned APK..."$WHITE
   $AAPT package -f -m -F $PROJECTDIR/bin/$APKNAME.unaligned.apk \
         -A $PROJECTDIR/assets -M $PROJECTDIR/AndroidManifest.xml -S $PROJECTDIR/res -I $SDK
         
   cp $PROJECTDIR/bin/classes.dex .
   $AAPT add $PROJECTDIR/bin/$APKNAME.unaligned.apk classes.dex

   ##### SIGNING #####
   # Sign APK, it's a MUST if you wanna install on phone!!!
   # Changed from 'apksigner' to 'jarsigner' on PC's/Pi to get apk's
   # installed in > Android 7.0 devices! If working with Android in TERMUX,
   # only TERMUX apksigner is used, it makes keystore generation,
   # signing and aligning in one turn!

   echo -e $GREEN"\n=> Signing and aligning APK..."$WHITE

   if [ $HOSTNAME != "Android" ];then

      # Generate new key pair for debug, switch off if you have one to sign the app!
      # Keystore password = key password = debug-test, alias = test1.

      echo -e "\n=> Generating keystore for signing..."
      keytool -genkey -noprompt -storepass debug-test \
              -keypass debug-test -alias test1 -dname "CN=uwe" \
              -validity 36500 -keystore $PROJECTDIR/$APPNAME.keystore \
              -keyalg RSA -keysize 2048

      #$APKSIGNER sign --ks $PROJECTDIR/$APPNAME.keystore --ks-pass pass:debug-test \
      #            $PROJECTDIR/bin/$APKNAME.unaligned.apk
      $APKSIGNER -verbose -keystore $PROJECTDIR/$APPNAME.keystore \
                 -storepass debug-test $PROJECTDIR/bin/$APKNAME.unaligned.apk test1

      # Align the APK (only works after signing)

      $ZIPALIGN -f 4 $PROJECTDIR/bin/$APKNAME.unaligned.apk \
             $PROJECTDIR/bin/$APKNAME.apk
   else
      $APKSIGNER -p debug-test $PROJECTDIR/$APPNAME.keystore \
      $PROJECTDIR/bin/$APKNAME.unaligned.apk \
      $PROJECTDIR/bin/$APKNAME.apk
      
   fi # of if [ $HOSTNAME != "Android" ]

fi # of if [ $ITEM != "$APPNAME-Run-without-compile" ]..

BUILDZEIT=$(date +%s)

##########################################################################
################ Start at last if selected to do so ######################
##########################################################################

case $ITEM in

	"$APPNAME-New_with_test_on_device")
		echo -e "\n=> Removing previous App installed...\n"
		adb uninstall $PACKAGENAME.$APPNAME
		echo -e "\n=> Installing and starting APK...\n"
		adb install $PROJECTDIR/bin/$APKNAME.apk
		adb shell am start -n $PACKAGENAME.$APPNAME/.MainActivity
		break;;
		
	"$APPNAME-New_with_test_on_device_with_LogCat")
		echo -e "\n=> Removing previous App installed...\n"
		adb uninstall $PACKAGENAME.$APPNAME
		echo -e "\n=> Installing and starting APK for debugging...\n"
		adb install $PROJECTDIR/bin/$APKNAME.apk
		adb shell am start -n $PACKAGENAME.$APPNAME/.MainActivity
		PID=$(adb jdwp)
		echo -e "App Process-ID: $PID"
		#adb logcat > logcat.txt
      adb logcat|grep $PID
      break;;
      
   "$APPNAME-App-Run-without-compile")
      adb shell am force-stop $PACKAGENAME.$APPNAME
      adb shell am start -n $PACKAGENAME.$APPNAME/.MainActivity
		break;;
esac

ENDZEIT=$(date +%s)
echo -e "\nTime spent:\n\n"$(echo $COMPILEZEIT " " $STARTZEIT | awk '{print($1-$2)}')" seconds for compiling."
echo -e $(echo $BUILDZEIT " " $COMPILEZEIT | awk '{print($1-$2)}')" seconds for creating & signing apk."
echo -e $(echo $ENDZEIT " " $STARTZEIT | awk '{print($1-$2)}')" seconds for whole build process.\n"
echo -e $BLUE"* $SCRIPTNAME: working on $APPNAME FINISHED! *"$WHITE

