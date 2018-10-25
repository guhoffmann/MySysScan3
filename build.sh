#!/bin/bash

################## build.sh (C) GU Hoffmann 05.06.2018 ###################
#
# Compile the Android App with linux commandline tools on any X86-PC,
# Raspberry Pi or on an Android device running TERMUX!
# You only need to have installed:
#
# - the android.jar of the desired Android platform (= PLATFORM_SDK)
# - the build tools aapt, zipalign, apksigner
# - jarsigner on non Android machines
#
###########################################################################

SCRIPTNAME="buildapp 18.10.25"

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
		GREP="$PREFIX/bin/grep --color"
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

PLATFORM_SDK="android-23-6.0.jar"

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
		SDK=/home/uwe/sparSDK/$PLATFORM_SDK
		PROJECTDIR=/home/uwe/MyDevelop/MyAndroid/$APPNAME
		JAVAC="ecj -source 1.7 -target 1.7 "
		;;

	uwe-nc10)
		SDK=/home/uwe/Android/SDK/sparSDK/$PLATFORM_SDK
		PROJECTDIR=/home/uwe/MyDevelop/MyAndroid/$APPNAME
		JAVAC="ecj -source 1.7 -target 1.7 "
		;;

	senior-medion)
		SDK=/home/uwe/sparSDK/$PLATFORM_SDK
		PROJECTDIR=/home/uwe/MyDevelop/MyAndroid/$APPNAME
		JAVAC="ecj -source 1.7 -target 1.7 "
		;;

	gamepi)
		SDK=/media/pi/kingston-8g-ext4/sparSDK/$PLATFORM_SDK
		PROJECTDIR=/media/pi/kingston-8g-ext4/MyDevelop/MyAndroid/$APPNAME
		JAVAC="ecj -source 1.7 -target 1.7 "
		;;

	Android)
		SDK=~/storage/shared/sparSDK/$PLATFORM_SDK
		PROJECTDIR=~/storage/downloads/MyDevelop/MyAndroid/$APPNAME
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
	rm $PROJECTDIR/output/*.*; \
	rm -r $PROJECTDIR/obj/*; \
	rm $PROJECTDIR/src/$PACKAGEPATH/$APPNAME/R.java; \
	find -L $PROJECTDIR -name \"classes.dex\" -delete \
"

# Now go on and select what to do...

if [ $HOSTNAME != "Android" ];then
   MENU="$APPNAME-New \
         $APPNAME-New_with_test_on_device \
         $APPNAME-New_with_test_on_device_with_LogCat \
         $APPNAME-Run-without-compile \
         $APPNAME-Clean \
         Abbruch"
else
   MENU="$APPNAME-New \
         $APPNAME-Run-without-compile \
         $APPNAME-Clean \
         Abbruch"
fi
      
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
COPTIONS=" "

##########################################################################
###################### Compile and create app ############################
##########################################################################

if [ $ITEM != "$APPNAME-Run-without-compile" ]\
   && [ $ITEM != "$APPNAME-Clean" ]; then

   STARTZEIT=$(date +%s)
   
   # Create output dir if not present
   
   if [ ! -d $PROJECTDIR/output ];then
        mkdir $PROJECTDIR/output
   fi

   # compile all resources in projects res dir to R.java
   # for accessing ressources from Java source code

   echo -e $GREEN"\n=> Creating R.java..."$WHITE
   aapt package -f -m -J $PROJECTDIR/src -M $PROJECTDIR/AndroidManifest.xml \
         -S $PROJECTDIR/res -I $SDK

   # Compile Java files

   echo -e $GREEN"\n=> Compiling java..."$WHITE
   $JAVAC $COPTIONS -d $PROJECTDIR/obj -classpath $PROJECTDIR/src: -bootclasspath $SDK \
        $PROJECTDIR/src/*.java 2>&1|$GREP -E '^|WARNING|ERROR'


   COMPILEZEIT=$(date +%s)

   # Make a dex file

   echo -e $GREEN"\n=> Making Dex..."$WHITE
 
   if [ $HOSTNAME != "Android" ];then
 		dalvik-exchange --dex --output=$PROJECTDIR/output/classes.dex $PROJECTDIR/obj
   else
		dx --dex --output=$PROJECTDIR/output/classes.dex $PROJECTDIR/obj
   fi

   # If you have the error UNEXPECTED TOP-LEVEL EXCEPTION, it can be because you use
   # old build tools and dalvik-exchange tries to translate java 1.7 rather than 1.8.
   # To solve the problem, you have to specify 1.7 java version in the previous
   # javac command:
   # javac -d obj -source 1.7 -target 1.7 ...

   ############################## Put everything in an APK #########################################

   echo -e $GREEN"\n=> Making unsigned APK..."$WHITE
   
   # First add resources to unaligned apk
   aapt package -f -m -F $PROJECTDIR/output/$APKNAME.unaligned.apk \
         -A $PROJECTDIR/assets -M $PROJECTDIR/AndroidManifest.xml \
         -S $PROJECTDIR/res -I $SDK
          
   # Now add DEX file to unaligned apk - don't know why I couldn't accomplish
   # this at once with command above - subject for research...
   cd $PROJECTDIR/output
   aapt add $PROJECTDIR/output/$APKNAME.unaligned.apk classes.dex
   cd $PROJECTDIR
   
   ###################################### SIGNING ##################################################
   # Sign APK, it's a MUST if you wanna install on phone!!!
   # Changed apksigner to jarsigner on PC/Pi to get apk's installed in > Android 6 devices!
   # If working with Android/TERMUX, only TERMUX apksigner is used.
   # It makes keystore generation, signing and aligning in one turn!

   echo -e $GREEN"\n=> Signing and aligning APK..."$WHITE

   if [ $HOSTNAME != "Android" ];then

      # Generate new key pair for debug, switch off if you have one to sign the app!
      # Keystore password = key password = alias: debug-test

      echo -e "\n=> Generating keystore for signing..."
      keytool -genkey -noprompt \
			  -alias debug-test \
			  -dname "CN=uwe" \
			  -storepass debug-test \
			  -keypass debug-test \
              -validity 36500 -keystore $PROJECTDIR/$APPNAME.keystore \
              -keyalg RSA -keysize 2048

	  # Dunno why apksigner doesn't work, must use jarsigner instead!
	  # APK signed with apksigner doesn't install, must work on this.
	  jarsigner -verbose -keystore $PROJECTDIR/$APPNAME.keystore \
				-storepass debug-test $PROJECTDIR/output/$APKNAME.unaligned.apk debug-test

      # Align the APK (only works after signing)
      zipalign -f 4 $PROJECTDIR/output/$APKNAME.unaligned.apk \
             $PROJECTDIR/output/$APKNAME.apk

   else  # $HOSTNAME = "Android"
	  apksigner -p debug-test $PROJECTDIR/$APPNAME.keystore \
      $PROJECTDIR/output/$APKNAME.unaligned.apk \
      $PROJECTDIR/output/$APKNAME.apk
      
   fi # of if [ $HOSTNAME != "Android" ]

fi # of if [ $ITEM != "$APPNAME-Run-without-compile" ]..

BUILDZEIT=$(date +%s)

##########################################################################
################ Start at last if selected to do so ######################
##########################################################################

# Configure install and run commands

if [ $HOSTNAME != "Android" ];then
   UNINSTALL="adb uninstall"
   INSTALL="adb install"
   STOP="adb shell am force-stop"
   RUN="adb shell am start -n"
else
   UNINSTALL="pm uninstall"
   INSTALL="pm install"
   STOP="am force-stop"
   RUN="am start"
fi

case $ITEM in

	"$APPNAME-New_with_test_on_device")
		echo -e "\n=> Removing previous App installed...\n"
		$UNINSTALL $PACKAGENAME.$APPNAME
		echo -e "\n=> Installing and starting APK...\n"
		$INSTALL $PROJECTDIR/output/$APKNAME.apk
		$RUN $PACKAGENAME.$APPNAME/.MainActivity
		break;;
		
	"$APPNAME-New_with_test_on_device_with_LogCat")
		echo -e "\n=> Removing previous App installed...\n"
		$UNINSTALL $PACKAGENAME.$APPNAME
		echo -e "\n=> Installing and starting APK for debugging...\n"
		$INSTALL $PROJECTDIR/output/$APKNAME.apk
		$RUN $PACKAGENAME.$APPNAME/.MainActivity
		PID=$(adb jdwp)
		echo -e "App Process-ID: $PID"
		#adb logcat > logcat.txt
        adb logcat
      break;;
      
   "$APPNAME-Run-without-compile")
      $STOP $PACKAGENAME.$APPNAME
      $RUN $PACKAGENAME.$APPNAME/.MainActivity
		break;;
esac

ENDZEIT=$(date +%s)
echo -e "\nTime spent:\n\n"$(echo $COMPILEZEIT " " $STARTZEIT | awk '{print($1-$2)}')" seconds for compiling."
echo -e $(echo $BUILDZEIT " " $COMPILEZEIT | awk '{print($1-$2)}')" seconds for creating & signing apk."
echo -e $(echo $ENDZEIT " " $STARTZEIT | awk '{print($1-$2)}')" seconds for whole build process.\n"
echo -e $BLUE"* $SCRIPTNAME: working on $APPNAME FINISHED! *"$WHITE
