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


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private final static int        PERMISSION_ID       = 44;
    public  final static String     RECEIVE_MAC_LIST    = "RECEIVE_MAC_LIST";
    private final static int        LOCATION_PERMISSION = 10;
    private final static String     FILE_URL            = "http://covid19.roinspace.com/index.php?download=PATIENTS";
    private final static boolean    HIDE_MAX_LIST       = false;

    private ArrayList<String>       listPermissions = new ArrayList<>();
    private IntentFilter            intentFilter    = new IntentFilter();

    private LocalBroadcastManager   bManager;
    private Context                 context;
    private TextView                detectedDevicesTextView;
    private ListView                listView;
    private Button                  scanDatabaseBtn;
    private Button                  viewStatisticsBtn;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        setupPermissions();

        setContentView(R.layout.activity_main);

        detectedDevicesTextView = findViewById(R.id.textView_detect_device);
        listView                = findViewById(R.id.listView);
        scanDatabaseBtn         = findViewById(R.id.databaseBtn);
        viewStatisticsBtn       = findViewById(R.id.verifyBtn);

        detectedDevicesTextView.setText(Globals.getStringInLocale("text_detected_devices_brief",this));
        scanDatabaseBtn.setText(Globals.getStringInLocale("button_verify_db", this));
        viewStatisticsBtn.setText(Globals.getStringInLocale("button_verify_stat", this));

        scanDatabaseBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                verifyDatabase();
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

        startBluetoothService();

        bManager = LocalBroadcastManager.getInstance(this);
        intentFilter.addAction(RECEIVE_MAC_LIST);
        bManager.registerReceiver(bReceiver, intentFilter);

        if(BluetoothService.mBTDeviceList.size() > 0)
            listView.setAdapter(new BTDeviceAdapter(context, BluetoothService.mBTDeviceList));
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

        if(BluetoothService.mBTDeviceList.size() > 0)
            listView.setAdapter(new BTDeviceAdapter(context, BluetoothService.mBTDeviceList));
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(RECEIVE_MAC_LIST)) {
                if(HIDE_MAX_LIST)
                    return;

                listView.setAdapter(new BTDeviceAdapter(context, BluetoothService.mBTDeviceList));

            }
        }
    };

    void startBluetoothService() {
        if (Build.VERSION.SDK_INT >= 26){
            startForegroundService(new Intent(this, BluetoothService.class));
        } else{
            startService(new Intent(this, BluetoothService.class));
        }
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
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                listPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
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


    }

}




