package MyTools;

import SystemCommandExecutor.*;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.*;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import android.hardware.camera2.*;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.Display;

import org.json.JSONException;
import org.json.JSONObject;

import static android.hardware.Camera.getCameraInfo;

//================================== HELPER FUNCTIONS ==============================================

public class MyTools {

    static int myToolsKilo = 1000; // multiplier for K,M,G
    public static String androidDevice; // "real"=real device, "emulator"=emulator, "virtualbox"
    private static HashMap<String, Integer> cpuCores;
    public static int numCores;
    private static ActivityManager.MemoryInfo memoryInfo;
    private static ActivityManager activityManager;
        

    // Code names for the different Android Versions

    public static String[] AndroidVersions = {"Base", "Base 1.1", "Cupcake", "Cur Development", "Donut",
            "Eclair", "Eclair 0.1", "Eclair M.R.1", "Foyo",
            "Gingerbead", "Gingerbead M.R.1", "Honeycomb", "Honeycomb M.R.1", "Honeycomb M.R.2",
            "Ice Cream Sandwich", "Ice Cream Sandwich M.R.1", "Jellybean", "Jellybean M.R.1", "Jellybean M.R.2",
            "Kitkat", "Kitkat Wear", "Lollipop", "Lollipop M.R.1",
            "Marshmallow", "Nougat", "Nougat M.R.1", "Oreo", "Oreo M.R.1","","",""};
 
     /** Call this from calling app to initialize some important values ****************************
     *
     */    
            
    public static void init(Context context) {
        
        numCores = Integer.parseInt(getPipedCmdLine("ls -d /sys/devices/system/cpu/cpu?|grep -c 'cpu'"));
        memoryInfo = new ActivityManager.MemoryInfo();
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    /** Get text output of a piped terminal command ************************************************
     *
     * @param cmdline  command line parameters
     * @return String
     */

    public static String getPipedCmdLine(String cmdline) {
        try {
            String result;
            // you need a shell to execute a command pipeline
            List<String> commands = new ArrayList<String>();
            commands.add("/system/bin/sh");
            commands.add("-c");
            commands.add(cmdline);

            SystemCommandExecutor commandExecutor = new SystemCommandExecutor(commands);
            commandExecutor.executeCommand();
            StringBuilder stdout = commandExecutor.getStandardOutputFromCommand();
            StringBuilder stderr = commandExecutor.getStandardErrorFromCommand();
            result = stdout.toString();
            Log.d("RESULT:",cmdline + " > " + result + " " + stderr.toString());
            if ( result.length()<2 ) {
                return "null";
            } else return result.substring(0,result.length()-1); // cut last char off!(CR)

        } catch (IOException e) {
            return "IOException in getPipedCmdLine!";
        } catch (InterruptedException i) {
            return "InterruptedException in getPipedCmdLine!";
        }
    } // of getPipedCmdLine(String cmdline)

    /** Calc value to GB, MB, KB, Bytes and return formatted String ********************************
     *
     * @param value long value to get formatted
     * @return String
     */

    public static String calcAmount(long value) {

        String retString;

        if ( value > Math.pow(myToolsKilo,3)) {
            retString = String.format(Locale.US, "%.2f G", value*1.0 / Math.pow(myToolsKilo,3));
        } else if ( value > Math.pow(myToolsKilo,2)) {
            retString = String.format(Locale.US, "%.2f M", value*1.0 / Math.pow(myToolsKilo,2));
        } else if ( value > myToolsKilo ) {
            retString = String.format(Locale.US, "%.2f K", value*1.0 / myToolsKilo);
        } else retString = String.format(Locale.US, "%d", value);

        return retString;

    } // of calcAmount(long value)

    /** Calc two long values to percentage and return formatted String *****************************
     *
     * @param zaehler, nenner
     * @return String
     */

    public static String calcProz(long zaehler, long nenner) {

        return String.format(Locale.US, "%.2f", zaehler*100.0/nenner) + " %";

    } // public static String calcProz(long zaehler, long nenner)...

    public static String fastRead(String filename) {
        String line;
        String retString = "";
        
        try {
            BufferedReader bf = new BufferedReader(new FileReader(filename));
            while ( (line = bf.readLine()) != null ){
                retString = retString + line;
            }
            bf.close();
        
        } catch (IOException e) {
            e.printStackTrace();
            retString = "file not found!";
        }
        
        return retString;
    } // of public static String fastRead(String filename)...
    
    /** Return processor info from cpuinfo as String ***********************************************
     *
     * @return String
     */

    public static String getCpu() {

        String cmdLine;
        String[] inString;

        cmdLine = getPipedCmdLine("grep '^model name' /proc/cpuinfo");
        if ( cmdLine.equals("null") ) cmdLine = getPipedCmdLine("cat /proc/cpuinfo|grep '^Processor'");

        inString = cmdLine.trim().split(":");
        if ( cmdLine.equals("null") ) {
            return "null";
        //} else return inString[1] + "\n(" + Build.SUPPORTED_ABIS[0] + ")"; // SUPPORTED_ABIS nicht für Android 4.4
        } else return inString[1] + "\n";

    } // of getCpu()

    /** Return IP adress ***************************************************************************
     *
     * @return String
     */

    public static String getIp() {

        String cmdLine;
        String[] inString;

        cmdLine = getPipedCmdLine("ip addr show|grep 'inet .*global'");

        inString = cmdLine.trim().split("\\s+");
        if ( cmdLine.equals("null") ) {
            return "null";
            //} else return inString[1] + "\n(" + Build.SUPPORTED_ABIS[0] + ")"; // SUPPORTED_ABIS nicht für Android 4.4
        } else return inString[1] + "\n";

    } // of getIp()

    /** Get hardware info (chipset?!) from cpuinfo as String ***************************************
     *
     * @return String
     */

    public static String getHardware() {

        String cmdLine;
        String[] inString;

        cmdLine = getPipedCmdLine("grep '^Hardware' /proc/cpuinfo");
        inString = cmdLine.trim().split(":");
                
        if (inString.length > 1) {
            return inString[1].trim();
        }
        else return "not found!";

    } // of getHardware()

    /** Calc processor number and frequencies and return formatted String **************************
     *
     * @return String
     */

    public static String getCpuCores(String androidDevice) {

        String cmdLine;
        String[] inString;
        long value;
        int helpInt;
        String retString = "";

        cpuCores = new HashMap<String, Integer>();

        if (androidDevice.equals("real") ) { // get system frequency infos for real device

            // Search for Cores and frequencies in system directories
            for (int i = 0;i< Integer.parseInt(getPipedCmdLine("ls -d /sys/devices/system/cpu/cpu?|grep -c 'cpu'"));i++) {

                cmdLine = getPipedCmdLine("cat /sys/devices/system/cpu/cpu" +Integer.toString(i) +"/cpufreq/cpuinfo_max_freq");
                value = Long.parseLong(cmdLine) * 1000;
                if ( cpuCores.containsKey(calcAmount(value))) { // add to frequency found
                    helpInt = cpuCores.get(calcAmount(value));
                    helpInt++;
                    cpuCores.put(calcAmount(value),helpInt);
                } else {  // new Frequency found!
                    cpuCores.put(calcAmount(value),1);
                }
            }// of for (int i = 0;i< Integer.parseInt(...

        } else { //get system frequency infos for android emulator

            cmdLine = getPipedCmdLine("cat /proc/cpuinfo|grep -w 'cpu MHz'");
            inString = cmdLine.trim().split(":");
            value = Math.round(Float.parseFloat(inString[1])*1000.0*1000.0);
            cpuCores.put(calcAmount(value),1);
        }

        for (HashMap.Entry<String, Integer> entry : cpuCores.entrySet()) {
            retString = retString + entry.getValue() + "@" + entry.getKey() + "Hz ";
        }
        return retString;

    } // of getCpuCores()
    
    /** Calc frequency of each core and return formatted String ************************************
     *
     * @return String
     */

    public static String getCpuCoresFreqs(String androidDevice) {

        String cmdLine;
        String[] inString;
        long value;
        String retString = "";

        if (androidDevice.equals("real") ) { // get system frequency infos for real device

            // Search for Cores and frequencies in system directories
            for (int i = 0;i< Integer.parseInt(getPipedCmdLine("ls -d /sys/devices/system/cpu/cpu?|grep -c 'cpu'"));i++) {

                cmdLine = getPipedCmdLine("cat /sys/devices/system/cpu/cpu" +Integer.toString(i) +"/cpufreq/scaling_cur_freq");
                retString = retString + String.format(Locale.US, "%05d", Long.parseLong(cmdLine)/1000) + " ";

            }// of for (int i = 0;i< Integer.parseInt(...

        } else { //get system frequency infos for android emulator - not adjusted for timer usage!

            cmdLine = getPipedCmdLine("cat /proc/cpuinfo|grep -w 'cpu MHz'");
            inString = cmdLine.trim().split(":");
            value = Math.round(Float.parseFloat(inString[1])*1000.0*1000.0);
            cpuCores.put(calcAmount(value),1);
        }

        return retString;

    } // of getCpuCoresFreqs()

    /** Read size of RAM using meminfo *************************************************************
     *
     * @param choice type of RAM
     * @return G,M,K formatted output of RAM (total, free, used)
     */

    public static String getRam(int choice) {

        String cmdLine;
        String[] inString;
        long value;

        switch(choice){
            case 0: cmdLine = getPipedCmdLine("cat /proc/meminfo|grep -w 'MemTotal:'");
                inString = cmdLine.trim().split("\\s+");
                value = Long.parseLong(inString[1]) * 1024;
                break;
            case 1: cmdLine = getPipedCmdLine("cat /proc/meminfo|grep -w 'Active:'");
                inString = cmdLine.trim().split("\\s+");
                value = Long.parseLong(inString[1]) * 1024;
                break;
            case 2: cmdLine = getPipedCmdLine("cat /proc/meminfo|grep -w 'MemTotal:'");
                inString = cmdLine.trim().split("\\s+");
                value = Long.parseLong(inString[1]) * 1024;
                cmdLine = getPipedCmdLine("cat /proc/meminfo|grep -w 'Active:'");
                inString = cmdLine.trim().split("\\s+");
                value = value - Long.parseLong(inString[1]) * 1024;
                break;
            case 3: cmdLine = getPipedCmdLine("cat /proc/meminfo|grep -w 'MemFree:'");
                inString = cmdLine.trim().split("\\s+");
                value = Long.parseLong(inString[1]) * 1024;
                break;
            default: value = 0;
                break;
        }

        return MyTools.calcAmount(value);

    } // of getRam(int choice)

    /** Read size of RAM using Java API ************************************************************
     *
     * @param choice type of RAM
     * @return G,M,K formatted output of RAM (total, free, used)
     */

    public static String getRam2(int choice) {

        long value;

        switch(choice){
            case 0:
                activityManager.getMemoryInfo(memoryInfo);
                value = memoryInfo.totalMem;
                break;
            case 1:
                activityManager.getMemoryInfo(memoryInfo);
                value = memoryInfo.availMem;
                break;
            case 2:
                activityManager.getMemoryInfo(memoryInfo);
                value = (memoryInfo.totalMem - memoryInfo.availMem);
                break;
            default: value = 0;
                break;
        }

        return calcAmount(value);

    } // of getRam2(int choice)
    
    /** Read used size of internal data partition on real Android device ***************************
     *
     * @return long
     */

    public static long getAndroidDataStorageUsed() {

        return getInternalStorage(1);

    } // of getAndroidSystemStorageUsed()


    /** Read size of internal storage **************************************************************
     *
     * @param choice defines type of storage
     * @return long
     */

    public static long getInternalStorage(int choice) {

        // getRootDirectory or getDataDirectory ?
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        long value;

        if (Build.VERSION.SDK_INT > 20) {
            switch (choice) {
                case 0:
                    value = stat.getBlockCountLong() * stat.getBlockSizeLong(); // not really 'total'!!!
                    break;
                case 1:
                    value = stat.getAvailableBlocksLong() * stat.getBlockSizeLong(); // available
                    break;
                case 2:
                    value = stat.getFreeBlocksLong() * stat.getBlockSizeLong(); // free
                    break;
                default:
                    value = 0;
                    break;
            } // of switch
        } else {
            switch (choice) {
                case 0:
                    value = (long) stat.getBlockCount() * stat.getBlockSize(); // not really 'total'!!!
                    break;
                case 1:
                    value = (long) stat.getAvailableBlocks() * stat.getBlockSize(); // available
                    break;
                case 2:
                    value = (long) stat.getFreeBlocks() * stat.getBlockSize(); // free
                    break;
                default:
                    value = 0;
                    break;
            } // of switch+ "\n Available: " +  MyTools.calcAmount(availableInternalStorage)
        } // if (Build.VERSION.SDK_INT > 20)... else...

        return value;

    } // of getInternalStorage(int choice)

    /** Read size of external storage **************************************************************
     *
     * @param choice defines type of storage
     * @return long
     */

    public static long getExternalStorage(int choice) {

        long value;

        if ( (MyTools.getPipedCmdLine("df|grep sdcard").equals("null") )
            && ( MyTools.getPipedCmdLine("cat /proc/partitions|grep -w mmcblk1").equals("null") )){
            value = 0;
        } else {

            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
            if (Build.VERSION.SDK_INT > 20) {

                switch (choice) {
                    case 0:
                        value = stat.getBlockSizeLong() * stat.getBlockCountLong(); // not really 'total'!!!
                        break;
                    case 1:
                        value = stat.getAvailableBlocksLong() * stat.getBlockSizeLong(); // available
                        break;
                    case 2:
                        value = stat.getFreeBlocksLong() * stat.getBlockSizeLong(); // free
                        break;
                    default:
                        value = 0;
                        break;
                } // of switch
            } else {
                switch (choice) {
                    case 0:
                        value = (long) stat.getBlockSize() * stat.getBlockCount(); // not really 'total'!!!
                        break;
                    case 1:
                        value = (long) stat.getAvailableBlocks() * stat.getBlockSize(); // available
                        break;
                    case 2:
                        value = (long) stat.getFreeBlocks() * stat.getBlockSize(); // free
                        break;
                    default:
                        value = 0;
                        break;
                } // of switch+ "\n Available: " +  MyTools.calcAmount(availableInternalStorage)
            } // if (Build.VERSION.SDK_INT > 20)... else...

        }

        return value;

    } // of getExternalStorage(int choice)

    /** Read total size of internal storage ********************************************************
     *
     * @return long
     */

    public static long getTotalInternalStorage() {
        String cmdLine;

         if (androidDevice == "real") {
            cmdLine = MyTools.getPipedCmdLine("cat /proc/partitions|grep -w mmcblk0"); //for Android
         } else if (androidDevice == "emulator") {
                cmdLine = MyTools.getPipedCmdLine("cat /proc/partitions|grep -w vda"); //for android emulator
         } else {
                cmdLine = MyTools.getPipedCmdLine("cat /proc/partitions|grep -w loop1"); //for virtualbox
        }

        String[] inString = cmdLine.trim().split("\\s+");

        return Long.parseLong(inString[2]) * 1024;

    } // of getTotalInternalStorage()

    /** Read total size of system storage on real Android device ***********************************
     *
     * @return long
     */

    public static long getAndroidSystemStorage() {
        String cmdLine;
        String[] inString;
        long value;

        // get /dev/block whole internal storage
        cmdLine = MyTools.getPipedCmdLine("cat /proc/partitions|grep -w mmcblk0");
        inString = cmdLine.trim().split("\\s+");
        value = Long.parseLong(inString[2]) * 1024;
        
        return value - getInternalStorage(1);

    } // of getAndroidSystemStorage()

    /** Get informations about cameras *************************************************************
     *
     */

    public static String getCameraInfos(Activity activity) {

        String result = "";

        if (Build.VERSION.SDK_INT > 20) { // approach for SDK > 20

            CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            Rect camRes;

            try {

                for (String cameraId : manager.getCameraIdList()) {
                    CameraCharacteristics chars
                            = manager.getCameraCharacteristics(cameraId);
                    if (chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                        result = result + "Front: ";
                    else result = result + "Back: ";
                    camRes = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                    result = result + camRes.width() + " x " + camRes.height()
                            + " (" + calcAmount(camRes.width() * camRes.height()) + "Pixel)\n";

                }

            } catch (CameraAccessException e) {

                e.printStackTrace();
                result = "no camera found!";

            }

        } else { // // approach for SDK < 21

            Camera camera;
            Size cameraSize;

            try {

                for(int i=0;i<Camera.getNumberOfCameras();i++){

                    camera = Camera.open(i);
                    final CameraInfo cameraInfo = new CameraInfo();
                    getCameraInfo(i,cameraInfo);

                    if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT)
                        result = result + "Front: ";
                    else result = result + "Back: ";
                    // To lazy to analyze all resolutions, take the first in list...
                    cameraSize =  camera.getParameters().getSupportedPictureSizes().get(0);
                    result = result + cameraSize.width + " x " + cameraSize.height
                            + " (" + calcAmount(cameraSize.width * cameraSize.height) + "Pixel)\n";
                }

            } catch (Exception e) {

                result = "no cameras found!";

            }

        } // of if (Build.VERSION.SDK_INT > 20) ... else { ...

        return result;

    } // of public static String getCameraInfos()

    /** Get informations about display *************************************************************
     *
     */

    public static String getDisplayInfos(Activity activity) {
       
       // Code geklaut aus Dalvik-Explorer!
       // Gets full screen resolution including ActionBar!
        
        Display display =  activity.getWindowManager().getDefaultDisplay();
        
      DisplayMetrics metrics = new DisplayMetrics();
      display.getMetrics(metrics);
      int widthPixels = metrics.widthPixels;
      int heightPixels = metrics.heightPixels;
      try {
      widthPixels = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
      heightPixels = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
      } catch (Exception ignored) {
      }
      try {
      Point realSize = new Point();
      Display.class.getMethod("getRealSize", Point.class).invoke(display, realSize);
      widthPixels = realSize.x;
      heightPixels = realSize.y;
      } catch (Exception ignored) {
      }
        
        return widthPixels
               + " x "
               + heightPixels
               + " Pixel";

    } // of public static String getDisplayInfos(Activity activity)

    /** Single core benchmark sieve of Erastothenes  ***********************************************
     *
     */

    public static String getPrimBench(long timeLength) {

        String result;
        long timeEnd;
        long iterations = 0; // number of iterations (to measure effective calculations!)
        int primzaehler=0; // how many numbers found?
        int zahl=1; // number to start with
        int primtrue;
        int zaehler;
        int testende;
        long timeStart = System.currentTimeMillis();
        long grenze = timeStart + timeLength; // how long to search for primes? About timelength/1000 s !

        while (System.currentTimeMillis()<grenze){

            // is zahl a prime number?

            testende=0;
            primtrue=1;
            zaehler=2;

            while ( (zaehler<=(int) Math.sqrt(zahl)) && (testende==0)){
                if (zahl%zaehler==0){
                    primtrue=0;
                    testende=1;
                } else zaehler++;
                iterations++;
            }

            if (primtrue==1){
                primzaehler++;
            }

            // test next zahl
            zahl++;

        } // of while...

        timeEnd = System.currentTimeMillis();
        result = "First " + primzaehler + " primes found in "
                + String.format(Locale.US, "%.1f",0.001*(double)(timeEnd - timeStart)) + " s\n"
                + "Factor: " + String.format(Locale.US, "%.2f",0.001*iterations/(double)(timeEnd - timeStart))
                + " (bigger is better)";

        return result;

    } // public static String getPrimBench(long timeLength)

    /** Single core benchmark composing a fourier series for a square signal ***********************
     *
     */

    public static String getSquareBench(long timeLength) {


        int grenze;  // limit of iterations/frequencies
        double step;
        double t;
        double f;
        int n;
        long timeEnd;
        long iterations = 0;
        long timeStart = System.currentTimeMillis();
        long timeGrenze = timeStart + timeLength; // how long to compose? About timelength/1000 s !
        String result;

        grenze=20000;
        step = 1.0/grenze;

        t = 0;

        // 1 period from t = 0-6

        while ( System.currentTimeMillis()<timeGrenze ) {

            f = 0;

            for ( n = 1; n<=grenze; n+=2 ) {
                f = f + 4/Math.PI * (Math.sin(n*t)/n);
                iterations++;
            }
            t+=step;
        }

        timeEnd = System.currentTimeMillis();
        result = iterations + " iterations in " + String.format(Locale.US, "%.1f",0.001*(double)(timeEnd - timeStart)) + " s\n"
                + "Factor: " + String.format(Locale.US, "%.1f",0.01*iterations/(double)(timeEnd - timeStart))
                + " (bigger is better)";

        return result;

    } // of public static String getSquareBench(long timeLength)

    /** parse JSON file in assets of application context 'myContext' *******************************
     *  and return a JSONObject
     *
     */

    public static JSONObject parseJSONData(Context myContext, String jsonFile) {

        String JSONString = null;
        JSONObject JSONObject = null;

        try {

            InputStream inputStream = myContext.getAssets().open(jsonFile);
            int sizeOfJSONFile = inputStream.available();

            // store data in this and read all data
            byte[] bytes = new byte[sizeOfJSONFile];
            inputStream.read(bytes);
            inputStream.close();
            // create JSON object with data of the byte array
            JSONString = new String(bytes, "UTF-8");
            JSONObject = new JSONObject(JSONString);

        } catch (IOException ex) {

            ex.printStackTrace();
            return null;

        } catch (JSONException x) {

            x.printStackTrace();
            return null;
        }

        return JSONObject;

    } // of public static JSONObject parseJSONData(Context myContext, String jsonFile)

} // of MyTools

/*=================================================================================================

                                    END OF CLASS MYTOOLS

==================================================================================================*/
