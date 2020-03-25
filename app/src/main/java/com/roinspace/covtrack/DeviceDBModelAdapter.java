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
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

public class DeviceDBModelAdapter extends ArrayAdapter<DeviceDBModel> {
    ArrayList<DeviceDBModel> dbModelList;
    Context context;

    public DeviceDBModelAdapter(Context context, ArrayList<DeviceDBModel> arrayList) {
        super(context,R.layout.list_row,arrayList);
        this.dbModelList =arrayList;
        this.context=context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        DeviceDBModel btDeviceModel= dbModelList.get(position);

        Log.d("getView",position+" "+btDeviceModel.macAddress);
        if(convertView==null) {

            LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView=layoutInflater.inflate(R.layout.list_row, null);
            convertView.setOnClickListener(new CustomClickListener(btDeviceModel));

            TextView tittle=convertView.findViewById(R.id.list_title);
            TextView subTitle=convertView.findViewById(R.id.list_subtitle);
            ImageView imag=convertView.findViewById(R.id.list_image);
            imag.setImageResource(R.drawable.infected);
            tittle.setText(btDeviceModel.macAddress);
            String duration = Globals.getStringInLocale("text_duration", context);
            subTitle.setText(duration + ": " + btDeviceModel.sessionDuration + " s    ");

        }
        return convertView;
    }

    class CustomClickListener implements View.OnClickListener
    {

        DeviceDBModel btDeviceModel;
        public CustomClickListener(DeviceDBModel btDeviceModel) {
            this.btDeviceModel = btDeviceModel;
        }

        @Override
        public void onClick(View v)
        {
            Intent intent = new Intent(context, MapsActivity.class);
            if(btDeviceModel.sessionStopLocation == null || (btDeviceModel.sessionStopLocation.getLongitude() == 0 && btDeviceModel.sessionStopLocation.getLatitude() == 0))
            {
                displayPatientLocationUnavailable();
                return;
            }
            intent.putExtra("patient_location",btDeviceModel.sessionStopLocation);
            context.startActivity(intent);
        }

    };

    void displayPatientLocationUnavailable() {
        TextView textView = new TextView(context);
        textView.setText(Globals.getStringInLocale("title_no_patient_location", context));
        textView.setPadding(40, 30, 20, 30);
        textView.setTextSize(17F);
        textView.setBackgroundColor(Color.WHITE);
        textView.setTextColor(Color.BLACK);

        new AlertDialog.Builder(context)
                .setCustomTitle(textView)
                .setMessage(Globals.getStringInLocale("text_no_patient_location", context))
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return dbModelList.size();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
    }

    @Override
    public int getCount() {
        return dbModelList.size();
    }

    @Override
    public DeviceDBModel getItem(int position) {
        return dbModelList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }


}
