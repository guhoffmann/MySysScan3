#!/bin/bash

################## build.sh (C) GU Hoffmann 05.06.2018 ###################
#
# Compile the Android App with linux commandline tools on any X86-PC,
# Raspberry Pi or on an Android device running TERMUX!
# You only need to have installed:
#
# - 'android.jar' of the desired Android platform (=SDKDIR)
# - build tools: 'aapt', 'apksigner', 'dx' (java .class to .dex compiler)
# - java compiler 'javac' ('ecj' for Android platforms)
#
###########################################################################

SCRIPTNAME="build 19.10.18-1"
. ./android.cfg # Include configurations

export PATH=$PATH:$BUILDTOOLSDIR

clear

# 'function' for cleaning project
CLEANFUNC="
	rm $PROJECTDIR/output/*.*; \
	rm -r $PROJECTDIR/obj/*; \
	rm $PROJECTDIR/src/$PACKAGEPATH/$APPNAME/R.java; \
	find -L $PROJECTDIR -name \"classes.dex\" -delete \
"

# Now go on and select what to do...

# construct the menu according to system (Android or Linux system)
HOSTNAME=$(uname -o)
if [ $HOSTNAME != "Android" ];then
   MENU="Build $APPNAME|Test $APPNAME on device|Build $APPNAME with test on device with LogCat|Run $APPNAME without build|Clean project dir $APPNAME"
else
   MENU="Build $APPNAME|Run $APPNAME without build|Clean project dir $APPNAME"
fi

#==========================================================================
# Display menu and wait for action to be selected with a key.
# This is repeated until 'q' is pressed to quit the script!
#==========================================================================

key=""
while [ "$key" != "q" ]; do

	echo "*** $SCRIPTNAME ***"
	echo
	echo "Host $HOSTNAME, select an option:"
	echo

	# Print Menu and wait for keypress
	echo $MENU | awk -F '|' '{ for(i=1;i<=NF;i++) print i") "$i }'
	echo "q) Ende"
	read -p "-> " key
	# store selection in ITEM
	ITEM=$(echo $key"|"$MENU|awk -F '|' '{ print $(1+$1)}')

	# call clean 'function' if necessary
	case $ITEM in
			
		"q")
			echo "Break!"
			exit;;
		"Run $APPNAME without build")
			;;
			
		*)
			bash -c "$CLEANFUNC"
			echo "Project cleaned!"
			;;
	esac
	clear

	#--------------------- Compile and create app -------------------------

	if [ "$ITEM" != "Run $APPNAME without build" ]\
		&& [ "$ITEM" != "Clean project dir $APPNAME" ]; then

		STARTTIME=$(date +%s)
		
		# Create output dir if not present
		
		if [ ! -d $PROJECTDIR/output ];then
			  mkdir $PROJECTDIR/output
		fi

		# compile all resources in projects res dir to R.java
		# for accessing ressources from Java source code
		echo
		echo "=> Creating R.java..."
		aapt package -f -m -J $PROJECTDIR/src -M $PROJECTDIR/AndroidManifest.xml \
				-S $PROJECTDIR/res -I $SDKDIR

		# Compile Java files
		echo
		echo "=> Compiling java..."
		$JAVAC -d $PROJECTDIR/obj -classpath $PROJECTDIR/src: -bootclasspath $SDKDIR \
			  $PROJECTDIR/src/*.java 2>&1|grep -E '^|WARNING|ERROR'


		COMPILETIME=$(date +%s)

		# Make a dex file
		echo
		echo "=> Making Dex..."

		if [ "$HOSTNAME" != "Android" ];then
			"$BUILDTOOLSDIR"dx --dex --output=$PROJECTDIR/output/classes.dex $PROJECTDIR/obj
		else
			dx --dex --output=$PROJECTDIR/output/classes.dex $PROJECTDIR/obj
		fi

		# If error UNEXPECTED TOP-LEVEL EXCEPTION occurs, the cause can be
		# old build tools and dalvik-exchange trying to translate java 1.7
		# rather than 1.8. To solve this problem, specify use of 1.7 java
		# version in the previous javac command:
		# 'javac -d obj -source 1.7 -target 1.7 ...'

		#--------------------- Put everything in an APK -------------------
		
		echo
		echo "=> Making unsigned APK..."
		
		# First add resources to unaligned apk
		if [ $HOSTNAME != "Android" ];then
			"$BUILDTOOLSDIR"aapt package -f -m -F $PROJECTDIR/output/$APKNAME.apk \
					-A $PROJECTDIR/assets -M $PROJECTDIR/AndroidManifest.xml \
					-S $PROJECTDIR/res -I $SDKDIR
			# Now add DEX file to unaligned apk - don't know why I couldn't accomplish
			# this at once with command above - subject for research...
			cd $PROJECTDIR/output
			"$BUILDTOOLSDIR"aapt add $PROJECTDIR/output/$APKNAME.apk classes.dex

		else
			aapt package -f -m -F $PROJECTDIR/output/$APKNAME.apk \
					-A $PROJECTDIR/assets -M $PROJECTDIR/AndroidManifest.xml \
					-S $PROJECTDIR/res -I $SDKDIR
			# Now add DEX file to unaligned apk - don't know why I couldn't accomplish
			# this at once with command above - subject for research...
			cd $PROJECTDIR/output
			aapt add $PROJECTDIR/output/$APKNAME.apk classes.dex

		fi
				 
		cd $PROJECTDIR
		
		#-------------------------- SIGNING -------------------------------
		# Signing the apk is a MUST to get it installed on phone!!!

		echo
		echo "=> Signing and aligning APK..."

		if [ $HOSTNAME != "Android" ];then

			# Generate new key pair for debug, switch off if you have one to sign the app!
			# Keystore password = key password = alias: debug-test

			echo
			echo "=> Generating keystore for signing..."
			keytool -genkey -noprompt \
				  -alias debug-test \
				  -dname "CN=uwe" \
				  -storepass debug-test \
				  -keypass debug-test \
					  -validity 36500 -keystore $PROJECTDIR/$APPNAME.keystore \
					  -keyalg RSA -keysize 2048

		  # apksigner for Linux and Android differ in their parameters!
		  "$BUILDTOOLSDIR"apksigner sign --ks-pass pass:debug-test --ks $PROJECTDIR/$APPNAME.keystore \
			$PROJECTDIR/output/$APKNAME.apk \
		
		else  # $HOSTNAME = "Android"
		   apksigner -p debug-test $PROJECTDIR/$APPNAME.keystore \
			$PROJECTDIR/output/$APKNAME.apk \
			$PROJECTDIR/output/$APKNAME-finished.apk
			
		fi # of if [ $HOSTNAME != "Android" ]

	fi # of if [ $ITEM != "$APPNAME-Run-without-compile" ]..

	BUILDTIME=$(date +%s)

	#--- Start at last if selected to do so and adb tools are installed ---

	# Configure install/run commands of adb tools

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

		"Test $APPNAME on device")
			echo
			echo "=> Removing previous App installed..."
			$UNINSTALL $PACKAGENAME.$APPNAME
			echo
			echo "=> Installing and starting APK..."
			$INSTALL $PROJECTDIR/output/$APKNAME.apk
			$RUN $PACKAGENAME.$APPNAME/.MainActivity
			;;
			
		"Build $APPNAME with test on device with LogCat")
			echo
			echo "=> Removing previous App installed..."
			$UNINSTALL $PACKAGENAME.$APPNAME
			echo
			echo "=> Installing and starting APK for debugging..."
			$INSTALL $PROJECTDIR/output/$APKNAME.apk
			$RUN $PACKAGENAME.$APPNAME/.MainActivity
			PID=$(adb jdwp)
			echo "App Process-ID: $PID"
			#adb logcat > logcat.txt
			 adb logcat
			;;
			
		"Run $APPNAME without build")
			$STOP $PACKAGENAME.$APPNAME
			$RUN $PACKAGENAME.$APPNAME/.MainActivity
			;;
	esac

	ENDTIME=$(date +%s)
	echo
	echo "Time spent: "$(echo $COMPILETIME " " $STARTTIME | awk '{print($1-$2)}')" seconds for compiling."
	echo $(echo $BUILDTIME " " $COMPILETIME | awk '{print($1-$2)}')" seconds for creating & signing apk."
	echo $(echo $ENDTIME " " $STARTTIME | awk '{print($1-$2)}')" seconds for whole build process."
	echo
	if [ -f $PROJECTDIR/output/$APKNAME.apk ]; then
		echo "Check file:"
		echo $(ls -l $PROJECTDIR/output/$APKNAME.apk)
		echo
	fi			
	echo
	if [ "$key" = "q" ]; then
		echo
		exit 0
	fi
	echo  "Working on $APPNAME FINISHED! *"
	echo
done
