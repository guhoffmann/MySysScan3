/*
 * MainActivity.java
 * 
 ********************* Main file of the app MySysScan3 *************************
 * 
 * Copyright 2018 GU Hoffmann
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 * 
 */
 
package com.uweandapp.MySysScan3;

import MyTools.*;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.app.ProgressDialog;
import android.app.Activity;
import android.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.os.Build;
import android.os.Bundle;
import android.widget.ExpandableListView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*==================================================================================================

                               MAIN ACTIVITY CLASS FOR PROGRAM

==================================================================================================*/

public class MainActivity extends Activity {

    long totalInternalStorage;
    long availableInternalStorage;
    long availableExternalStorage;
    long freeInternalStorage;
    long sysInternalStorage;
    long androidSystemStorage;
    long androidDataStorageUsed;
    long totalExternalStorage;
    long freeExternalStorage;
    int periodicTime  = 2000; // ~2s time difference between two measurements!
    int firstPeriodic = 0; // has the periodic routine been running for at least one time?
    int runRefresh    = 0;
    int runPeriodical = 0;

    private com.uweandapp.MySysScan3.MyExpandableListAdapter listAdapter;
    private ExpandableListView expListView;
    private List<String> listDataHeader;
    private HashMap<String, List<String>> listDataChild;
    private JSONObject socObject;
    
    private List<String> ramResultList     = new ArrayList<String>();
    private List<String> deviceList        = new ArrayList<String>();
    private List<String> benchResultList   = new ArrayList<String>();
    private List<String> storageResultList = new ArrayList<String>();
    private List<String> socList           = new ArrayList<String>();
        
    private Handler periodicHandler;

    /** Class for async task calculating benchmarks and new values *********************************
     *
     * Create the task with new readNewValuesClass().execute(par) -> par = childelement to work on!
     * This is the Void-Element 'position' given to doInBackground(Integer... position)!
     *
     * In AsyncTask<Void, Integer, Void>:
     * - first Void parameter ist the one given to doInBackground(...)
     * - second Integer parameter is given from publishProgress to onProgressUpdate
     *
     */

    private class readNewValuesClass extends AsyncTask<Void, Integer, Void> {

        /** progress dialog to show user that the backup is processing. */
        private ProgressDialog dialog;
        String dialogMessage;
        
        /** application context !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
         * DO NOT REMOVE THIS DESPITE THE COMPILER WARNING IT'S NOT USED!!!
         * This code will not work without this activity statement. */
        private Activity activity;
        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

        public readNewValuesClass(Activity activity) {
           this.activity = activity;
           dialog = new ProgressDialog(activity);
           dialog.setCancelable(false);
        }

        // do this with the UI BEFORE processing async task!
        @Override
        protected void onPreExecute() {
            
            runRefresh = 1;
            
            ramResultList.clear();
            deviceList.clear();
            benchResultList.clear();
            storageResultList.clear();
            
            this.dialog.setTitle("Reading System data...");
            this.dialog.setMessage("Preparing\n◻◻◻◻◻◻◻◻◻◻");
            this.dialog.show();
        } // of onPreExecute()

        // do this DURING processing async task!
        @Override
        protected Void doInBackground(Void... p) {

            //-------------- everything that must be done to refresh fluent data -------------------

            dialogMessage = "Getting OS data\n◼◻◻◻◻◻◻◻◻◻";
            publishProgress(10);

            // get device informations

            deviceList.add("System|" + android.os.Build.MANUFACTURER+ " "
                    + android.os.Build.MODEL + " '" + MyTools.getPipedCmdLine("getprop ro.product.name") + "'");
            deviceList.add(" Ser.Nr.|" + android.os.Build.SERIAL);
            deviceList.add("OS|Android " + Build.VERSION.RELEASE + " "
                    + MyTools.AndroidVersions[Build.VERSION.SDK_INT]
                    + " API " + Build.VERSION.SDK_INT);
            deviceList.add("Build|" + MyTools.getPipedCmdLine("getprop ro.build.description"));
            deviceList.add("Build-Date|" + MyTools.getPipedCmdLine("getprop ro.build.date"));
            deviceList.add("Hostname|" + MyTools.getPipedCmdLine("getprop net.hostname"));
            //if (MyTools.getIp() != "null") {
                deviceList.add("Host-IP|" + MyTools.getIp());
            //}

            dialogMessage = "Getting storage information\n◼◼◻◻◻◻◻◻◻◻";
            publishProgress(20);
            
            // get storage informations

            totalInternalStorage     = MyTools.getTotalInternalStorage();
            totalExternalStorage     = MyTools.getExternalStorage(0);
            availableInternalStorage = MyTools.getInternalStorage(1);
            freeInternalStorage      = MyTools.getInternalStorage(2);
            freeExternalStorage      = MyTools.getExternalStorage(2);
            availableExternalStorage = MyTools.getExternalStorage(1);


            if ((MyTools.androidDevice.equals("emulator")) || (MyTools.androidDevice.equals("virtualbox"))) { // storage of emulator device!

                storageResultList.add("Total|" + MyTools.calcAmount(totalInternalStorage + totalExternalStorage - freeInternalStorage - freeExternalStorage)
                        + "B/" + MyTools.calcAmount(totalInternalStorage + totalExternalStorage) + "B used ("
                        + MyTools.calcProz(totalInternalStorage + totalExternalStorage - freeInternalStorage - freeExternalStorage,
                        totalInternalStorage + totalExternalStorage)
                        + ")");
                storageResultList.add("Internal|" + MyTools.calcAmount(totalInternalStorage - freeInternalStorage)
                        + "B/" + MyTools.calcAmount(totalInternalStorage) + "B used ("
                        + MyTools.calcProz(totalInternalStorage - freeInternalStorage,
                        totalInternalStorage)
                        + ")\n (System " + MyTools.calcAmount(sysInternalStorage) + "B)");
                storageResultList.add("Card|" + MyTools.calcAmount(totalExternalStorage - freeExternalStorage)
                        + "B/" + MyTools.calcAmount(totalExternalStorage) + "B used ("
                        + MyTools.calcProz(totalExternalStorage - freeExternalStorage,
                        totalExternalStorage)
                        + ")");

            } else { // storage of real Android device

                androidSystemStorage = MyTools.getAndroidSystemStorage();
                androidDataStorageUsed = MyTools.getAndroidDataStorageUsed();
                storageResultList.add("Total|" + MyTools.calcAmount(totalInternalStorage + totalExternalStorage - availableInternalStorage - availableExternalStorage)
                        + "B/" + MyTools.calcAmount(totalInternalStorage + totalExternalStorage) + "B used ("
                        + MyTools.calcProz(totalInternalStorage + totalExternalStorage - availableInternalStorage - availableExternalStorage,
                        totalInternalStorage + totalExternalStorage)
                        + ")\n" +  MyTools.calcAmount(availableInternalStorage + availableExternalStorage) + "B available");
                storageResultList.add("Internal|" + MyTools.calcAmount(totalInternalStorage - availableInternalStorage)
                                        + "B/" + MyTools.calcAmount(totalInternalStorage) + "B used ("
                                        + MyTools.calcProz(totalInternalStorage - availableInternalStorage,totalInternalStorage)
                                        + ")\n" +  MyTools.calcAmount(availableInternalStorage) + "B available");

                if ( totalExternalStorage == 0 ) {
                    storageResultList.add("Card|no extra SD-Card found!");
                } else
                    storageResultList.add("Card|" + MyTools.calcAmount(totalExternalStorage - freeExternalStorage)
                            + "B/" + MyTools.calcAmount(totalExternalStorage) + " used ("
                            + MyTools.calcProz(totalExternalStorage - freeExternalStorage,
                            totalExternalStorage)
                            + ")\n" +  MyTools.calcAmount(availableExternalStorage) + "B available");
            }


            dialogMessage = "Getting RAM information\n◼◼◼◼◻◻◻◻◻◻";
            publishProgress(30);
            
            ramResultList.add("Total|" + MyTools.getRam2(0) + "B on device" );
            ramResultList.add("Used|" + MyTools.getRam2(2) + "B" );
            ramResultList.add("Free|" + MyTools.getRam2(1) + "B");
            
            // precalc benchmark to get more reliable results
            dialogMessage = "Primes benchmark\n◼◼◼◼◼◻◻◻◻◻";
            publishProgress(40);
            // primes benchmark
            dialogMessage = "Primes benchmark\n◼◼◼◼◼◼◼◻◻◻";
            publishProgress(50);
            MyTools.getPrimBench(2000);
            benchResultList.add("Primes|" + MyTools.getPrimBench(2000));
            
            // precalc benchmark to get more reliable results
            dialogMessage = "Fourier benchmark\n◼◼◼◼◼◼◼◼◻◻";
            publishProgress(60);
            MyTools.getSquareBench(2000);
            // fourier benchmark
            dialogMessage = "Fourier benchmark\n◼◼◼◼◼◼◼◼◼◻";
            publishProgress(70);
            benchResultList.add("Fourier|" + MyTools.getSquareBench(2000));

            //-------------------- everything is done to refresh fluent data -----------------------

            dialogMessage = "Fourier benchmark\n◼◼◼◼◼◼◼◼◼◼";
            publishProgress(100);
            
            return null;

        } // of doInBackground(Integer... position)

        // do this to update processing status of async task!
        protected void onProgressUpdate(Integer... progress) {

            //Log.d("PROGRESS:",progress[0].toString());
            this.dialog.setMessage(dialogMessage);

        } // onProgressUpdate(Integer... progress)

        // do this with the UI AFTER processing async task!
        @Override
        protected void onPostExecute(Void p) {

            // put global device properties
            listDataChild.put(listDataHeader.get(0), deviceList);

            // put RAM results
            listDataChild.put( listDataHeader.get(2), ramResultList );

            // put storage results
            listDataChild.put( listDataHeader.get(3), storageResultList );

            // put benchmark results
            listDataChild.put( listDataHeader.get(5), benchResultList );

            listAdapter.notifyDataSetChanged();
            
            if (dialog.isShowing()) {
               dialog.dismiss();
            }
            // periodic handler must be called AFTER initialisation
            // of all list fields it should later work with!!!
            // So it's invoked here for the first time.
            /*if (firstPeriodic == 0) {
                periodicHandler.post(periodicalCode);
                firstPeriodic = 1;
             }*/
            
            runRefresh = 0; // Set the run marker to "finished"
            periodicHandler.post(periodicalCode);
            
        } // of onPostExecute(Void p)

    } // of readNewValuesClass ---------------------------------------------------------------------
    
    /** Class repating code periodically ***********************************************************
    *
    */
     
    private Runnable periodicalCode = new Runnable() {
        @Override
        public void run() {
            
            // Only run this if there's no Refresh-Class running!
            // Necessary to avoid conflicts and crash if both classes
            // try to access the List Adapter at the same time!!!
            
            if ( runRefresh == 0 ) {
				
				runPeriodical = 1; // mark periodical as running!
                ramResultList.set(1,"Used|" + MyTools.getRam2(2) + "B" );
                ramResultList.set(2,"Free|" + MyTools.getRam2(1) + "B");
                listDataChild.put( listDataHeader.get(2), ramResultList );

                String freqString = "Freqs|";

                for (int i = 0;i< MyTools.numCores;i++) {
                freqString = freqString +" "+MyTools.fastRead("/sys/devices/system/cpu/cpu" + i +"/cpufreq/scaling_cur_freq");
                }
                socList.set(4,freqString);
                listDataChild.put( listDataHeader.get(1), socList);

                listAdapter.notifyDataSetChanged();
                // repeat code every periodicTime milliseconds
                periodicHandler.postDelayed(this, periodicTime);
                
                runPeriodical = 0; // mark periodical as finished!
            }
        }
            
    }; // of periodicalCode ------------------------------------------------------------------------
    
    //============================== START / INITIALIZE THE APP ====================================
    //
    // Do everything that must be done at the start of the APP!
    //
    //==============================================================================================

    // Create the options menu in toolbar

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       super.onCreateOptionsMenu(menu);
       
       MenuInflater inflater = getMenuInflater();
       inflater.inflate(R.menu.menu_options, menu);
        return true;
    }

    // Create callbacks for options menu items

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_refresh:
				// only run this if periodical isn't running
				// to avoid crash and conflicts accessing list adapter!!!
				if (runPeriodical == 0) { // only run this if periodical isn't running!
					new readNewValuesClass(this).execute();
					// important to update the view after the list ist updated!
					listAdapter.notifyDataSetChanged();
				}
                return true;
            case R.id.action_info:
               showInfo(getString(R.string.main_app) +"\n"
                         + "version " + getString(R.string.app_version) + "\n"
                         + "\n\u00A9 GU Hoffmann 2018");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // First init tools library
        MyTools.init(this);

        // get JSON-Object containing the SOC infos
        socObject = MyTools.parseJSONData(this, "soc.json");

        // determine if we got an emulator or real device!

        if ( Integer.parseInt(MyTools.getPipedCmdLine("cat /proc/partitions|grep -wc mmcblk0")) == 1 ) {
            MyTools.androidDevice = "real";
            sysInternalStorage = MyTools.getAndroidSystemStorage();
        } else if ( Integer.parseInt(MyTools.getPipedCmdLine("cat /proc/partitions|grep -wc loop1")) == 1 ) {
            MyTools.androidDevice = "virtualbox";
            sysInternalStorage = 0;
        } else {
            MyTools.androidDevice = "emulator";
            sysInternalStorage = 0;
        }

        // set the View as defined in actvity_main.xml
        setContentView(R.layout.activity_main);

        // set up the progress window popup

        //=============== CONSTRUCTION OF LISTVIEW AND CORESSPONDING LISTENERS! ====================

        // get the listview from resources files
        expListView = (ExpandableListView) findViewById(R.id.lvExp);

        // prepare list data
        prepareListData();

        // make and set list adapter for explistview
        listAdapter = new com.uweandapp.MySysScan3.MyExpandableListAdapter(this, listDataHeader, listDataChild);
        expListView.setAdapter(listAdapter);

        //============= READ THE UPDATABLE DATA FOR THE FIRST TIME AT STARTUP!!! ===================

        // async tasks must always be newly constructed and can run only once!
        // otherwise you'll get an runtime exception!!!
        new readNewValuesClass(this).execute();
        
        // Create handler and start it for periodic refreshing of things...
        periodicHandler = new Handler();
        //periodicHandler.post(periodicalCode);
        
    } // of onCreate(Bundle savedInstanceState) ----------------------------------------------------

    //========================== PREPARE LIST DATA AND BUILD LIST ==================================

    private void prepareListData() {

        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        // Adding child data

        if ( MyTools.androidDevice.equals("real") ) {
            listDataHeader.add(" Device");
        } else {
            listDataHeader.add(" Emulated Device");
        }

        //--------------------- Global device and os informations ----------------------------------

        List<String> geraet = new ArrayList<String>();
        geraet.add("System|" + android.os.Build.MANUFACTURER+ " "
                + android.os.Build.MODEL + " '" + MyTools.getPipedCmdLine("getprop ro.product.name") + "'");
        geraet.add(" Ser.Nr.|" + android.os.Build.SERIAL);
        geraet.add("OS|Android " + Build.VERSION.RELEASE + " "
                + MyTools.AndroidVersions[Build.VERSION.SDK_INT]
                + " API " + Build.VERSION.SDK_INT);
        geraet.add("Hostname|" + MyTools.getPipedCmdLine("getprop net.hostname"));
        geraet.add("Build|" + MyTools.getPipedCmdLine("getprop ro.build.description"));
        geraet.add("Build-Date|" + MyTools.getPipedCmdLine("getprop ro.build.date"));

        listDataChild.put(listDataHeader.get(0), geraet);

        //------------------------- SoC/Processor informations -------------------------------------

        // try to get the SoC info from m< soc.json JSON file
        String socString = "";
        String socResult = "";
        String gpuResult = "";
        String cpuInfoHardware = MyTools.getHardware();

        try {
            int i = 0;
            int pruefLen = 0;
            JSONArray keys = socObject.names();

            while ( (i <= keys.length()) ) {
                socString = socObject.getJSONObject(keys.getString(i)).getString("chipset_model");
                if ( cpuInfoHardware.contains(socString) || Build.BOARD.contains(socString)) {
                    if ( socString.length() > pruefLen) {
                        socResult = "\nguessed: "
                                + socObject.getJSONObject(keys.getString(i)).getString("soc_name");
                        gpuResult = socObject.getJSONObject(keys.getString(i)).getString("gpu_name");
                        pruefLen = socString.length();
                    }
                }
                i++;
            }
        } catch (JSONException ex) {

        }

        listDataHeader.add(" SoC/Processor");
        if (MyTools.androidDevice.equals("real")) {
            socList.add("Hardware|" + cpuInfoHardware + socResult);
        }
        socList.add("Board|"  + android.os.Build.HARDWARE.trim() + " " + Build.BOARD.trim());
        socList.add("CPU|" + MyTools.getCpu().trim());
        socList.add("Core(s)|" + MyTools.numCores + ": " + MyTools.getCpuCores(MyTools.androidDevice) );
        socList.add("Freqs|" + MyTools.getCpuCoresFreqs(MyTools.androidDevice) );
        if ( socResult != "" ) {
            socList.add("GPU|" + "guessed: " + gpuResult);
        }
        listDataChild.put(listDataHeader.get(1), socList);

        //----------------------------- RAM informations -------------------------------------------

        listDataHeader.add(" RAM Memory");
        // values are put in readNewValuesClass() for being able to be updated on demand!

        //-------------------------- get storage informations --------------------------------------

        listDataHeader.add(" Storage");
        // values are put in readNewValuesClass() for being able to be updated on demand!

        //------------------------ Periphals: Camera, Display etc. ---------------------------------

        listDataHeader.add(" Periphals");
        List<String> periphalItem = new ArrayList<String>();
        periphalItem.add("Camera|" + MyTools.getCameraInfos(this));
        periphalItem.add("Display|" + MyTools.getDisplayInfos(this));
        listDataChild.put(listDataHeader.get(4), periphalItem);

        //---------------------------------- Benchmarks --------------------------------------------

        listDataHeader.add(" Benchmarks");
        // values are put in readNewValuesClass() for being able to be updated on demand!

    } // of prepareListData()

    /** Display alert dialog with message **********************************************************
     *
     */

    public void showInfo(String message ) {

        // set up the info popup dialog
   
        AlertDialog infoDialog = new AlertDialog.Builder(MainActivity.this).create();
        infoDialog.setTitle("Info!");
        infoDialog.setMessage(message);
        infoDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
									}
								});
        infoDialog.show();

    } // of public void showInfo(String message )

} // of MainActivity

/*=================================================================================================

                                  END OF CLASS MAINACTIVITY

==================================================================================================*/
