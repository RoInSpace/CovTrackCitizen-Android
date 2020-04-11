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

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

public class dbUtil extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "main_database";

    private static final String MAIN_TABLE = "devices";

    private static final String KEY_ID  = "id";
    private static final String KEY_MAC = "mac";

    private static final String KEY_START_CURRENT_SESSION_DATE      =   "start_current_session_date";
    private static final String KEY_STOP_CURRENT_SESSION_DATE       =   "stop_current_session_date";
    private static final String KEY_START_CURRENT_SESSION_LATITUDE  =   "start_current_session_latitude";
    private static final String KEY_START_CURRENT_SESSION_LONGITUDE =   "start_current_session_longitude";
    private static final String KEY_STOP_CURRENT_SESSION_LATITUDE   =   "stop_current_session_latitude";
    private static final String KEY_STOP_CURRENT_SESSION_LONGITUDE  =   "stop_current_session_longitude";
    private static final String KEY_CURRENT_SESSION_DURATION        =   "current_session_duration";

    private static final String KEY_START_MAX_SESSION_DATE      =   "start_max_session_date";
    private static final String KEY_STOP_MAX_SESSION_DATE       =   "stop_max_session_date";
    private static final String KEY_START_MAX_SESSION_LATITUDE  =   "start_max_session_latitude";
    private static final String KEY_START_MAX_SESSION_LONGITUDE =   "start_max_session_longitude";
    private static final String KEY_STOP_MAX_SESSION_LATITUDE   =   "stop_max_session_latitude";
    private static final String KEY_STOP_MAX_SESSION_LONGITUDE  =   "stop_max_session_longitude";
    private static final String KEY_MAX_SESSION_DURATION        =   "max_session_duration";

    private static final String LANGUAGE_TABLE = "language";

    private static final String KEY_LANGUAGE        = "language";
    private static final String KEY_LANGUAGE_UNIQUE = "type";

    private static final String DEVICES_TABLE_STRUCTURE = KEY_MAC+"`," +
            "`"+KEY_START_CURRENT_SESSION_DATE+"`," +
            "`"+KEY_STOP_CURRENT_SESSION_DATE+"`," +
            "`"+KEY_START_CURRENT_SESSION_LATITUDE+"`," +
            "`"+KEY_START_CURRENT_SESSION_LONGITUDE+"`," +
            "`"+KEY_STOP_CURRENT_SESSION_LATITUDE+"`," +
            "`"+KEY_STOP_CURRENT_SESSION_LONGITUDE+"`," +
            "`"+KEY_CURRENT_SESSION_DURATION+"`," +
            "`"+KEY_START_MAX_SESSION_DATE+"`," +
            "`"+KEY_STOP_MAX_SESSION_DATE+"`," +
            "`"+KEY_START_MAX_SESSION_LATITUDE+"`," +
            "`"+KEY_START_MAX_SESSION_LONGITUDE+"`," +
            "`"+KEY_STOP_MAX_SESSION_LATITUDE+"`," +
            "`"+KEY_STOP_MAX_SESSION_LONGITUDE+"`," +
            "`"+KEY_MAX_SESSION_DURATION+"`";


    public dbUtil(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        switch(DATABASE_VERSION) {

            case 1: {

                String CREATE_CONTACTS_TABLE = "CREATE TABLE " + MAIN_TABLE + "(" +
                        KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        KEY_MAC + " TEXT UNIQUE," +
                        KEY_START_CURRENT_SESSION_DATE + " TEXT," +
                        KEY_STOP_CURRENT_SESSION_DATE + " TEXT,"+
                        KEY_START_CURRENT_SESSION_LATITUDE +" REAL," +
                        KEY_START_CURRENT_SESSION_LONGITUDE +" REAL," +
                        KEY_STOP_CURRENT_SESSION_LATITUDE +" REAL," +
                        KEY_STOP_CURRENT_SESSION_LONGITUDE +" REAL," +
                        KEY_CURRENT_SESSION_DURATION +" INTEGER," +
                        KEY_START_MAX_SESSION_DATE + " TEXT," +
                        KEY_STOP_MAX_SESSION_DATE + " TEXT,"+
                        KEY_START_MAX_SESSION_LATITUDE +" REAL," +
                        KEY_START_MAX_SESSION_LONGITUDE +" REAL," +
                        KEY_STOP_MAX_SESSION_LATITUDE +" REAL," +
                        KEY_STOP_MAX_SESSION_LONGITUDE +" REAL," +
                        KEY_MAX_SESSION_DURATION +" INTEGER" +")";
                db.execSQL(CREATE_CONTACTS_TABLE);

                String CREATE_LANGUAGE_TABLE = "CREATE TABLE " + LANGUAGE_TABLE + "(" +
                        KEY_LANGUAGE_UNIQUE + " TEXT UNIQUE," +
                        KEY_LANGUAGE + " TEXT"+ ")";
                db.execSQL(CREATE_LANGUAGE_TABLE);
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + MAIN_TABLE);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + MAIN_TABLE);
        onCreate(db);
    }

    void setAppLanguage(String language) {

        SQLiteDatabase db = this.getWritableDatabase();

        String sqlCommand = "INSERT OR REPLACE INTO "+LANGUAGE_TABLE+" (`type`,`language`) VALUES ('language','"+language+"');";
        db.execSQL(sqlCommand);

        db.close();
    }

    String getAppLanguage() {

        String result       =  null;
        String sqlCommand   = "";

        SQLiteDatabase db = this.getWritableDatabase();


        sqlCommand = "SELECT * FROM "+LANGUAGE_TABLE+";";
        Cursor resultSet = db.rawQuery(sqlCommand, null);

        if(resultSet.getCount() > 0) {
            resultSet.moveToFirst();
            result = resultSet.getString(resultSet.getColumnIndex(KEY_LANGUAGE));
        }

        resultSet.close();
        db.close();

        return result;
    }

    void addSingleDevice(BTDevice device) {

        String sqlCommand       = "";
        String subSqlCommand    = "";
        Cursor resultSet        = null;

        SQLiteDatabase db = this.getWritableDatabase();

        sqlCommand = "SELECT "+KEY_MAX_SESSION_DURATION+" FROM "+MAIN_TABLE+" WHERE "+KEY_MAC+" ='" + device.getAddress() +"';";
        resultSet = db.rawQuery(sqlCommand, null);

        if(resultSet.moveToNext() == false)
        {
            subSqlCommand = subSqlCommand.concat("('"+
                    device.getAddress()+"','"+
                    device.getStartCurrentSessionDate().toString()+"','"+
                    device.getStopCurrentSessionDate().toString()+"','"+
                    device.getStartCurrentSessionLocation().getLongitude()+"','"+
                    device.getStartCurrentSessionLocation().getLongitude()+"','"+
                    device.getStopCurrentSessionLocation().getLongitude()+"','"+
                    device.getStopCurrentSessionLocation().getLatitude()+"','"+
                    device.getCurrentSessionDuration()+"','"+

                    device.getStartMaximumSessionDate().toString()+"','"+
                    device.getStopMaximumSessionDate().toString()+"','"+
                    device.getStartMaximumSessionLocation().getLatitude()+"','"+
                    device.getStartMaximumSessionLocation().getLongitude()+"','"+
                    device.getStopMaximumSessionLocation().getLatitude()+"','"+
                    device.getStopMaximumSessionLocation().getLongitude()+"','"+
                    device.getMaximumSessionDuration()+
                    "')");


            sqlCommand = "INSERT OR REPLACE INTO "+MAIN_TABLE+" " +
                    "(`"+ DEVICES_TABLE_STRUCTURE +
                    ") VALUES "+subSqlCommand+";";
            try {
                db.execSQL(sqlCommand);
            } catch (SQLException e)  {
                e.printStackTrace();
            }

            resultSet.close();
            db.close();

            return;
        }
        
        resultSet.moveToFirst();
        int lastMaximumSessionDuration = resultSet.getInt(0);
        resultSet.close();

        if(lastMaximumSessionDuration < device.getMaximumSessionDuration())
        {
            subSqlCommand = subSqlCommand.concat("('"+
                    device.getAddress()+"','"+
                    device.getStartCurrentSessionDate().toString()+"','"+
                    device.getStopCurrentSessionDate().toString()+"','"+
                    device.getStartCurrentSessionLocation().getLongitude()+"','"+
                    device.getStartCurrentSessionLocation().getLongitude()+"','"+
                    device.getStopCurrentSessionLocation().getLongitude()+"','"+
                    device.getStopCurrentSessionLocation().getLatitude()+"','"+
                    device.getCurrentSessionDuration()+"','"+

                    device.getStartMaximumSessionDate().toString()+"','"+
                    device.getStopMaximumSessionDate().toString()+"','"+
                    device.getStartMaximumSessionLocation().getLatitude()+"','"+
                    device.getStartMaximumSessionLocation().getLongitude()+"','"+
                    device.getStopMaximumSessionLocation().getLatitude()+"','"+
                    device.getStopMaximumSessionLocation().getLongitude()+"','"+
                    device.getMaximumSessionDuration()+
                    "')");


            sqlCommand = "INSERT OR REPLACE INTO "+MAIN_TABLE+" " +
                    "(`"+ DEVICES_TABLE_STRUCTURE +
                    ") VALUES "+subSqlCommand+";";
            try {
                db.execSQL(sqlCommand);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        else
        {
            sqlCommand = "SELECT * FROM "+MAIN_TABLE+" WHERE "+KEY_MAC+" ='" + device.getAddress() +"';";
            resultSet = db.rawQuery(sqlCommand, null);
            resultSet.moveToFirst();

            subSqlCommand = subSqlCommand.concat("('"+
                    device.getAddress()+"','"+
                    device.getStartCurrentSessionDate().toString()+"','"+
                    device.getStopCurrentSessionDate().toString()+"','"+
                    device.getStartCurrentSessionLocation().getLongitude()+"','"+
                    device.getStartCurrentSessionLocation().getLongitude()+"','"+
                    device.getStopCurrentSessionLocation().getLongitude()+"','"+
                    device.getStopCurrentSessionLocation().getLatitude()+"','"+
                    device.getCurrentSessionDuration()+"','"+
                    resultSet.getString(resultSet.getColumnIndex(KEY_START_MAX_SESSION_DATE))+"','"+
                    resultSet.getString(resultSet.getColumnIndex(KEY_STOP_MAX_SESSION_DATE))+"','"+
                    resultSet.getString(resultSet.getColumnIndex(KEY_START_MAX_SESSION_LATITUDE))+"','"+
                    resultSet.getString(resultSet.getColumnIndex(KEY_START_MAX_SESSION_LONGITUDE))+"','"+
                    resultSet.getString(resultSet.getColumnIndex(KEY_STOP_MAX_SESSION_LATITUDE))+"','"+
                    resultSet.getString(resultSet.getColumnIndex(KEY_STOP_MAX_SESSION_LONGITUDE))+"','"+
                    resultSet.getString(resultSet.getColumnIndex(KEY_MAX_SESSION_DURATION))+
                    "')");

            sqlCommand = "INSERT OR REPLACE INTO "+MAIN_TABLE+" " +
                    "(`"+ DEVICES_TABLE_STRUCTURE +
                    ") VALUES "+subSqlCommand+";";

            try {
                db.execSQL(sqlCommand);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        db.close();
    }

    void addDevices(HashMap<String, BTDevice> devices) {

        String sqlCommand       = "";
        String subSqlCommand    = "";

        SQLiteDatabase db = this.getWritableDatabase();


        for(Map.Entry<String, BTDevice> entry : devices.entrySet()) {
            String mac = entry.getKey();
            BTDevice device = entry.getValue();

            subSqlCommand = subSqlCommand.concat("('"+
                    mac+"','"+
                    device.getStartCurrentSessionDate().toString()+"','"+
                    device.getStopCurrentSessionDate().toString()+"','"+
                    device.getStartCurrentSessionLocation().getLongitude()+"','"+
                    device.getStartCurrentSessionLocation().getLongitude()+"','"+
                    device.getStopCurrentSessionLocation().getLongitude()+"','"+
                    device.getStopCurrentSessionLocation().getLatitude()+"','"+
                    device.getCurrentSessionDuration()+"','"+

                    device.getStartMaximumSessionDate().toString()+"','"+
                    device.getStopMaximumSessionDate().toString()+"','"+
                    device.getStartMaximumSessionLocation().getLatitude()+"','"+
                    device.getStartMaximumSessionLocation().getLongitude()+"','"+
                    device.getStopMaximumSessionLocation().getLatitude()+"','"+
                    device.getStopMaximumSessionLocation().getLongitude()+"','"+
                    device.getMaximumSessionDuration()+
                    "'),");
        }
        subSqlCommand = subSqlCommand.substring(0, subSqlCommand.length() - 1);

        sqlCommand = "INSERT OR REPLACE INTO "+MAIN_TABLE+" " +
                "(`"+ DEVICES_TABLE_STRUCTURE +
                ") VALUES "+subSqlCommand+";";
        db.execSQL(sqlCommand);

        db.close();
    }

    HashMap<String,String> getDevicesInfo(Vector<String> macList) {

        String sqlCommand       = "";
        String subSqlCommand    = "";
        Cursor resultSet        = null;

        SQLiteDatabase db = this.getWritableDatabase();

        HashMap<String, String> devicesInfo = new HashMap<>();

        subSqlCommand = "(";
        for(int i = 0; i < macList.size()-1; i++) {
            subSqlCommand = subSqlCommand.concat("'"+macList.elementAt(i)+"',");
        }
        subSqlCommand = subSqlCommand.concat("'"+macList.elementAt(macList.size()-1)+"')");

        sqlCommand = "SELECT * FROM "+MAIN_TABLE+" WHERE `mac` IN "+subSqlCommand+";";
        resultSet = db.rawQuery(sqlCommand, null);
        while(resultSet.moveToNext()) {
            String mac = resultSet.getString(resultSet.getColumnIndex(KEY_MAC));


            String deviceInfo = "";
            deviceInfo += resultSet.getString(resultSet.getColumnIndex(KEY_MAC)) + "\n" +
                   /* "Start curr " + resultSet.getString(resultSet.getColumnIndex(KEY_START_CURRENT_SESSION_DATE)) +"\n" +
                    "Stop curr " + resultSet.getString(resultSet.getColumnIndex(KEY_STOP_CURRENT_SESSION_DATE)) +"\n" +
                    "session: " + resultSet.getString(resultSet.getColumnIndex(KEY_CURRENT_SESSION_DURATION)) + "\n"+*/
                    "Start max" + resultSet.getString(resultSet.getColumnIndex(KEY_START_MAX_SESSION_DATE)) +"\n" +
                    "Stop max" + resultSet.getString(resultSet.getColumnIndex(KEY_STOP_MAX_SESSION_DATE)) +"\n" +
                    "Max session: " + resultSet.getString(resultSet.getColumnIndex(KEY_MAX_SESSION_DURATION)) + "\n";

            devicesInfo.put(mac, deviceInfo);
        }

        resultSet.close();
        db.close();

        return devicesInfo;
    }

    ArrayList<DeviceDBModel> getDevices(Vector<String> macList) {

        if(macList.size() == 0)
            return new ArrayList<DeviceDBModel>();

        String sqlCommand       = "";
        String subSqlCommand    = "";
        Cursor resultSet        = null;

        SQLiteDatabase db = this.getWritableDatabase();

        ArrayList<DeviceDBModel> devices = new ArrayList<>();

        subSqlCommand = "(";
        for(int i = 0; i < macList.size()-1; i++) {
            subSqlCommand = subSqlCommand.concat("'"+macList.elementAt(i)+"',");
        }
        subSqlCommand = subSqlCommand.concat("'"+macList.elementAt(macList.size()-1)+"')");

        sqlCommand = "SELECT * FROM "+MAIN_TABLE+" WHERE `mac` IN "+subSqlCommand+";";
        resultSet = db.rawQuery(sqlCommand, null);
        while(resultSet.moveToNext()) {
            String mac = resultSet.getString(resultSet.getColumnIndex(KEY_MAC));

            DeviceDBModel device = new DeviceDBModel();

            device.macAddress = resultSet.getString(resultSet.getColumnIndex(KEY_MAC));

            device.sessionStartDate = resultSet.getString(resultSet.getColumnIndex(KEY_START_MAX_SESSION_DATE));
            device.sessionStopDate = resultSet.getString(resultSet.getColumnIndex(KEY_STOP_MAX_SESSION_DATE));

            device.sessionStartLocation.setLatitude(resultSet.getDouble(resultSet.getColumnIndex(KEY_START_MAX_SESSION_LATITUDE)));
            device.sessionStartLocation.setLongitude(resultSet.getDouble(resultSet.getColumnIndex(KEY_START_MAX_SESSION_LONGITUDE)));

            device.sessionStopLocation.setLatitude(resultSet.getDouble(resultSet.getColumnIndex(KEY_STOP_MAX_SESSION_LATITUDE)));
            device.sessionStopLocation.setLongitude(resultSet.getDouble(resultSet.getColumnIndex(KEY_STOP_MAX_SESSION_LONGITUDE)));

            device.sessionDuration = resultSet.getInt(resultSet.getColumnIndex(KEY_MAX_SESSION_DURATION));

            devices.add(device);
        }

        resultSet.close();
        db.close();

        return devices;
    }
}
