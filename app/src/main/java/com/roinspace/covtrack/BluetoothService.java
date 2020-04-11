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

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class BluetoothService extends Service {

    private static String       TAG           = "BLUETOOTH_BACKGROUND_SERVICE";
    private static String       CHANNEL_ID    = "COVID_APP";

    private static boolean      ENABLE_SCREEN_ON_OFF = false;
    private final static int    BLUETOOTH_SCANNING_INTERVAL = 20*1000;
    private static final int    RSSI_MAX           = -50;
    private final static int    RSSI_MIN           = -80;
    private final static int    COVID_DURATION     = 60 ;
    private final static int    BTDEVICE_TIMEOUT   = 100 * 1000;

    public static HashMap<String,BTDevice>      mBTDeviceList   = new HashMap<String,BTDevice>();
    private IntentFilter                        filter          = new IntentFilter();

    private BluetoothAdapter        mBluetoothAdapter;
    private Timer                   timer;
    private Date                    lastDiscoveryStart;
    private Context                 serviceContext;
    public Location                 lastKnownLocation;

    private FusedLocationProviderClient mFusedLocationClient;

    public BluetoothService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        super.onStartCommand(intent, flags, startId);

        if (intent.getAction().equals(MainActivity.START_FOREGROUND_ACTION)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("").setContentText("").build();
                startForeground(1, notification);
            }
        }
        else if (intent.getAction().equals( MainActivity.STOP_FOREGROUND_ACTION)) {
            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Create Bluetooth Service");

        serviceContext = this;

        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mReceiver, filter);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setBluetoothEnable();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                checkLocationAvailability();

                lastDiscoveryStart = new Date();

                if(ENABLE_SCREEN_ON_OFF) {
                    PowerManager pm = (PowerManager) serviceContext.getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
                    wl.acquire();
                    wl.release();
                }

                setBluetoothEnable();

                if(!isBluetoothDiscoverable())
                    setBluetoothDiscoverable();

                mBluetoothAdapter.startDiscovery();

            }
        }, 0, BLUETOOTH_SCANNING_INTERVAL);
    }

    @Override
    public void onDestroy() {

        Log.d(TAG, "Destroy Bluetooth Service");

        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }

        if(timer != null) {
            timer.cancel();
            timer = null;
        }

        unregisterReceiver(mReceiver);
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    public void getLastLocation(){
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.getLastLocation().addOnCompleteListener(
                        new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                Location location = task.getResult();

                                if (location == null) {
                                    requestNewLocationData();
                                }
                                else
                                    setLastKnownLocation(location);
                            }
                        }
                );
            }
        }
    }

    private void requestNewLocationData(){

        LocationRequest mLocationRequest = new LocationRequest();

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(0);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        );

    }

    private void setLastKnownLocation(Location l){
        lastKnownLocation = new Location(l);
    }

    boolean checkLocationAvailability() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            return false;
        else
            return true;
    }

    private void setBluetoothEnable() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager != null)
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled()) {

            mBluetoothAdapter.enable();
        }
    }

    private void setBluetoothDiscoverable() {

        try {
            Method bluetoothDeviceVisibility;
            bluetoothDeviceVisibility = mBluetoothAdapter.getClass().getMethod("setScanMode", int.class, int.class);
            bluetoothDeviceVisibility.invoke(mBluetoothAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private boolean isBluetoothDiscoverable() {
        if(mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
            return false;
        else
            return true;
    }

    public  BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if(ENABLE_SCREEN_ON_OFF) {
                if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    PowerManager pm = (PowerManager) serviceContext.getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG);
                    wl.acquire();
                    wl.release();

                }
            }

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG,"Bluetooth off");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG,"Turning Bluetooth off...");
                        break;
                    case BluetoothAdapter.STATE_ON:
                    {
                        if(!isBluetoothDiscoverable())
                            setBluetoothDiscoverable();
                        mBluetoothAdapter.startDiscovery();
                    }
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG,"Turning Bluetooth on...");
                        break;
                }
            }

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                BluetoothDevice device          = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Location        deviceLocation  = new Location("");
                int             rssi            = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MAX_VALUE);

                if(checkLocationAvailability())
                {
                    getLastLocation();
                    if(lastKnownLocation != null)
                        deviceLocation = lastKnownLocation;
                }

                if(mBTDeviceList.containsKey(device.getAddress())) {
                    mBTDeviceList.get(device.getAddress()).updateDevice( new BTDevice(mBTDeviceList.get(device.getAddress()),new Date(),deviceLocation,rssi));
                    Log.d("Discovery","Old active: "+device.getAddress());
                } else {
                    mBTDeviceList.put(device.getAddress(), new BTDevice(device.getAddress(),new Date(),deviceLocation,rssi));
                    mBTDeviceList.get(device.getAddress()).updateDevice();
                    Log.d("Discovery","New: "+device.getAddress());
                }

                for(Map.Entry<String, BTDevice> entry : mBTDeviceList.entrySet()) {
                    String key = entry.getKey();

                    Log.d("HashMapMember",key+" "+entry.getValue().getStopCurrentSessionDate());

                    if ((entry.getValue().getCurrentSessionDuration() > COVID_DURATION) /*&& (entry.getValue().getMax_RSSI() > RSSI_MIN)) || (entry.getValue().getMax_RSSI() > RSSI_MAX)*/) {
                        Log.d("DB INSERT", "NEW DEVICE TO BE CONSIDERED: "+ entry.getValue().getAddress() + " " +entry.getValue().getMaxRSSI() +" "+entry.getValue().getStopCurrentSessionLocation().getLatitude()+" "+entry.getValue().getStopCurrentSessionLocation().getLongitude());

                        dbUtil databaseConn = new dbUtil(getApplicationContext());
                        databaseConn.addSingleDevice(entry.getValue());
                        databaseConn.close();
                    }

                    if (entry.getValue().getStopCurrentSessionDate().getTime() < lastDiscoveryStart.getTime()-BLUETOOTH_SCANNING_INTERVAL)
                        entry.getValue().setStatus(BTDevice.IS_OFF);
                    else
                        entry.getValue().setStatus(BTDevice.IS_ACTIVE);

                }

                Log.d("BT found", device.getName() + "\n" + device.getAddress()+ " " + rssi);

                Intent macAddressIntent = new Intent(MainActivity.RECEIVE_MAC_LIST);
                LocalBroadcastManager.getInstance(context).sendBroadcast(macAddressIntent);

            }
        }
    };

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            setLastKnownLocation(locationResult.getLastLocation());
        }
    };
}
