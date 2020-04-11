/*
 * CovTrack - an app logging Bluetooth devices in your vicinity to monitor infection progress of COVID-19
 * Copyright (C) 2020  Romanian InSpace Engineering

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */

package com.roinspace.covtrack;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity {

    private final static int        PERMISSION_ID           = 44;
    public  final static String     RECEIVE_MAC_LIST        = "RECEIVE_MAC_LIST";
    private final static int        LOCATION_PERMISSION     = 10;
    private final static String     FILE_URL                = "";
    public final static String      START_FOREGROUND_ACTION = "START_FOREGROUND";
    public final static String      STOP_FOREGROUND_ACTION  = "STOP_FOREGROUND";


    private final String[] languages = {"English","Romana","Info"};

    private ArrayList<String>       listPermissions = new ArrayList<>();
    private IntentFilter            intentFilter    = new IntentFilter();

    private LocalBroadcastManager   bManager;
    private Context                 context;
    private TextView                detectedDevicesTextView;
    private ImageButton             scanDatabaseBtn;
    private ImageButton             viewStatisticsBtn;
    private ImageButton             runScanBtn;
    private ImageButton             languageBtn;
    private ProgressDialog          progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        setupPermissions();

        setContentView(R.layout.activity_main_beta);

        detectedDevicesTextView = findViewById(R.id.textView_detect_device);
        scanDatabaseBtn         = findViewById(R.id.databaseBtn);
        viewStatisticsBtn       = findViewById(R.id.verifyBtn);
        runScanBtn              = findViewById(R.id.button2);
        languageBtn             = findViewById(R.id.button3);

        languageBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setItems(languages, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        switch(item) {
                            case 0: {
                                Globals.LANGUAGE = "en";
                                dbUtil databaseConn = new dbUtil(getApplicationContext());
                                databaseConn.setAppLanguage(Globals.LANGUAGE);
                                databaseConn.close();
                                recreate();
                            }
                            break;
                            case 1: {
                                Globals.LANGUAGE = "ro";
                                dbUtil databaseConn = new dbUtil(getApplicationContext());
                                databaseConn.setAppLanguage(Globals.LANGUAGE);
                                databaseConn.close();
                                recreate();
                            }
                            break;
                            case 2:{
                                Intent infoActivityIntent = new Intent(context,InfoActivity.class);
                                startActivity(infoActivityIntent);
                            }
                        }
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        if(isBluetoothServiceRunning("com.roinspace.covtrack.BluetoothService"))
        {
            runScanBtn.setImageResource(R.drawable.stop_scan_btn);
            String text = Globals.getStringInLocale("bluetooth_name",context) + BluetoothAdapter.getDefaultAdapter().getName() + "\n";
            text+= Globals.getStringInLocale("text_scanning_devices_brief",context) +" ";
            text+=String.valueOf(BluetoothService.mBTDeviceList.size()) + " "+Globals.getStringInLocale("device_placeholder",context);
            detectedDevicesTextView.setText(text);
        }
        else
        {
            runScanBtn.setImageResource(R.drawable.start_scan_btn);
            String text = Globals.getStringInLocale("bluetooth_name",context) + BluetoothAdapter.getDefaultAdapter().getName() + "\n";
            text+= Globals.getStringInLocale("text_no_scanning_devices_brief",context);
            detectedDevicesTextView.setText(text);
        }

        runScanBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                if(isBluetoothServiceRunning("com.roinspace.covtrack.BluetoothService"))
                {
                    String text = Globals.getStringInLocale("bluetooth_name",context) + BluetoothAdapter.getDefaultAdapter().getName() + "\n";
                    text+= Globals.getStringInLocale("text_no_scanning_devices_brief",context);
                    detectedDevicesTextView.setText(text);
                    runScanBtn.setImageResource(R.drawable.start_scan_btn);
                    stopBluetoothService();
                }
                else
                {
                    String text = Globals.getStringInLocale("bluetooth_name",context) + BluetoothAdapter.getDefaultAdapter().getName() + "\n";
                    text+= Globals.getStringInLocale("text_scanning_devices_brief",context) +" ";
                    text+=String.valueOf(BluetoothService.mBTDeviceList.size()) + " "+Globals.getStringInLocale("device_placeholder",context);
                    detectedDevicesTextView.setText(text);
                    runScanBtn.setImageResource(R.drawable.stop_scan_btn);
                    startBluetoothService();
                }
            }
        });



        if(Globals.LANGUAGE.compareTo("ro") == 0)
        {
            scanDatabaseBtn.setImageResource(R.drawable.database_btn_selector_ro);
            viewStatisticsBtn.setImageResource(R.drawable.statistics_btn_selector_ro);
        }
        else
        {
            scanDatabaseBtn.setImageResource(R.drawable.database_btn_selector_en);
            viewStatisticsBtn.setImageResource(R.drawable.statistics_btn_selector_en);
        }



        scanDatabaseBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(!isNetworkAvailable())
                {
                    displayAlertNoInternet();
                    return;
                }

                progressBar = new ProgressDialog(v.getContext());
                progressBar.setCancelable(true);
                progressBar.setMessage("File downloading ...");
                progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressBar.setIndeterminate(true);
                progressBar.show();

                verifyFirebase();
            }
        });

        viewStatisticsBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(context, StatisticsWebActivity.class);
                startActivity(intent);
            }
        });


        bManager = LocalBroadcastManager.getInstance(this);
        intentFilter.addAction(RECEIVE_MAC_LIST);
        bManager.registerReceiver(bReceiver, intentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               // getLastLocation();
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();

        checkLocationAvailability();

    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private boolean isBluetoothServiceRunning(String serviceName){

        boolean serviceRunning  = false;
        ActivityManager am      = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);

        List<ActivityManager.RunningServiceInfo> l      = am.getRunningServices(50);
        Iterator<ActivityManager.RunningServiceInfo> it = l.iterator();

        while (it.hasNext()) {
            ActivityManager.RunningServiceInfo runningServiceInfo = it
                    .next();

            if(runningServiceInfo.service.getClassName().equals(serviceName)){
                serviceRunning = true;

                if(runningServiceInfo.foreground)
                {
                    //service run in foreground
                }
            }
        }
        return serviceRunning;
    }

        private BroadcastReceiver bReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(RECEIVE_MAC_LIST)) {

                    String text = Globals.getStringInLocale("bluetooth_name",context) + BluetoothAdapter.getDefaultAdapter().getName() + "\n";
                    text+= Globals.getStringInLocale("text_scanning_devices_brief",context) +" ";
                    text+=String.valueOf(BluetoothService.mBTDeviceList.size()) + " "+Globals.getStringInLocale("device_placeholder",context);
                    detectedDevicesTextView.setText(text);
                }
            }
        };

    FirebaseUtil.DatabasePatientsListener callback = new FirebaseUtil.DatabasePatientsListener() {
        @Override
        public void onReceivePatientsSuccess(Vector<String> patients) {
            dbUtil db = new dbUtil(context);
            ArrayList<DeviceDBModel> infectedPatients = db.getDevices(patients);

            db.close();
            progressBar.dismiss();
            if(infectedPatients.size() == 0)
                displayAlertNotInfected();
            else
            {
                displayAlertInfected(infectedPatients);
            }
        }

        @Override
        public void onReceivePatientsFailed() {
            displayDBError();
        }
    };

    void startBluetoothService() {
        Intent startIntent = new Intent(this, BluetoothService.class);
        startIntent.setAction(START_FOREGROUND_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            startForegroundService(startIntent);
        } else{
            startService(startIntent);
        }
    }

    void stopBluetoothService() {

        Intent stopIntent = new Intent(this, BluetoothService.class);

        stopIntent.setAction(STOP_FOREGROUND_ACTION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(stopIntent);
        else
            startService(stopIntent);

    }

    void checkLocationAvailability() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            displayRequestLocationEnable();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    private void verifyFirebase() {

        FirebaseUtil.getPatients(callback);
    }

    private void verifyDatabase() {
        String output;
        if(!isNetworkAvailable())
        {
            displayAlertNoInternet();
            return;
        }

        try {
            output = new DownloadTask().execute(FILE_URL).get();

            if(output.compareTo("No Internet Connection") == 0)
            {
                displayAlertNoInternet();
                return;
            }

            JSONArray obj = new JSONArray(output);
            dbUtil db = new dbUtil(context);
            Vector<String> list = new Vector<String>();


            if(obj.length() == 0) {
                displayAlertNotInfected();
                return;
            }

            for(int i = 0; i < obj.length(); i++){
                list.add(obj.getJSONObject(i).getString("mac_id"));
            }

            ArrayList<DeviceDBModel> infectedPatients = db.getDevices(list);
            db.close();

            if(infectedPatients.size() == 0)
                displayAlertNotInfected();
            else
            {

                Intent intent = new Intent(context, InfectedPatientsActivity.class);
                intent.putParcelableArrayListExtra("patient_array",infectedPatients);
                startActivity(intent);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
            displayDBError();
        }
    }

    void displayRequestLocationEnable() {
        TextView textView = new TextView(context);
        textView.setText(Globals.getStringInLocale("title_no_location", this));
        textView.setPadding(40, 30, 20, 30);
        textView.setTextSize(17F);
        textView.setBackgroundColor(Color.WHITE);
        textView.setTextColor(Color.BLACK);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(Globals.getStringInLocale("text_no_location", this))
                .setCustomTitle(textView)
                .setCancelable(false)
                .setPositiveButton(Globals.getStringInLocale("button_confirm", this), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton(Globals.getStringInLocale("button_deny", this), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    void displayDBError() {
        TextView textView = new TextView(context);
        textView.setText(Globals.getStringInLocale("title_db_error", this));
        textView.setPadding(40, 30, 20, 30);
        textView.setTextSize(17F);
        textView.setBackgroundColor(Color.WHITE);
        textView.setTextColor(Color.BLACK);

        new AlertDialog.Builder(context)
                .setCustomTitle(textView)
                .setMessage(Globals.getStringInLocale("text_db_error", this))
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    void displayAlertInfected(final ArrayList<DeviceDBModel> infectedPatients) {
        TextView textView = new TextView(context);
        textView.setText(Globals.getStringInLocale("title_interact", this));
        textView.setPadding(40, 30, 20, 30);
        textView.setTextSize(17F);
        textView.setBackgroundColor(Color.WHITE);
        textView.setTextColor(Color.BLACK);

        new AlertDialog.Builder(context)
                .setCustomTitle(textView)
                .setMessage(Globals.getStringInLocale("text_interact", this))
                .setCancelable(false)
                .setPositiveButton(Globals.getStringInLocale("verify_list",this), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(context, InfectedPatientsActivity.class);
                        intent.putParcelableArrayListExtra(InfectedPatientsActivity.RECEIVE_PATIENT_LIST,infectedPatients);
                        startActivity(intent);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    void displayAlertNotInfected() {
        TextView textView = new TextView(context);
        textView.setText(Globals.getStringInLocale("title_no_interact", this));
        textView.setPadding(40, 30, 20, 30);
        textView.setTextSize(17F);
        textView.setBackgroundColor(Color.WHITE);
        textView.setTextColor(Color.GREEN);

        new AlertDialog.Builder(context)
                .setCustomTitle(textView)
                .setMessage(Globals.getStringInLocale("text_no_interact", this))
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    void displayAlertNoInternet() {
        TextView textView = new TextView(context);
        textView.setText(Globals.getStringInLocale("text_no_network", this));
        textView.setPadding(40, 30, 20, 30);
        textView.setTextSize(17F);
        textView.setBackgroundColor(Color.WHITE);
        textView.setTextColor(Color.BLACK);

        new AlertDialog.Builder(context)
                .setCustomTitle(textView)
                .setMessage(Globals.getStringInLocale("text_no_network", this))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    void setupPermissions() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            listPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            listPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            listPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
                listPermissions.add(Manifest.permission.FOREGROUND_SERVICE);
            }
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                listPermissions.add(Manifest.permission.READ_PHONE_STATE);
            }
            if (!listPermissions.isEmpty()) {
                requestPermissions(listPermissions.toArray(new String[listPermissions.size()]), LOCATION_PERMISSION);

            }
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }


    }

}




