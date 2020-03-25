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
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;

class BTDeviceAdapter extends BaseAdapter {
    private HashMap<String,BTDevice>    deviceList;
    private Context                     context;
    private String[]                    mKeys;

    public BTDeviceAdapter(Context context, HashMap<String,BTDevice> arrayList) {
        //super(context,R.layout.list_row,arrayList);
        this.deviceList =arrayList;
        this.mKeys = arrayList.keySet().toArray(new String[arrayList.size()]);
        this.context=context;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        String key = mKeys[position];
        BTDevice bt=getItem(position);

        Log.d("getView",position+" "+bt.getAddress());

        if(convertView==null) {

            LayoutInflater layoutInflater = LayoutInflater.from(context);

            convertView=layoutInflater.inflate(R.layout.list_row, null);
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });

            TextView tittle=convertView.findViewById(R.id.list_title);
            TextView stittle=convertView.findViewById(R.id.list_subtitle);
            ImageView imag=convertView.findViewById(R.id.list_image);

            tittle.setText(bt.getAddress());
            String duration = Globals.getStringInLocale("text_duration", context);
            String proximity = Globals.getStringInLocale("text_proximity", context);
            stittle.setText(duration + ": " + bt.getCurrentSessionDuration() + " s    " + proximity + ": " + bt.roughDistance(context)+" ("+bt.getMaxRSSI()+" dBm)");

            if (bt.getStatus() == BTDevice.IS_ACTIVE)
                imag.setImageResource(R.drawable.bt_on);
            else
                imag.setImageResource(R.drawable.bt_off);
        }
        return convertView;
    }
    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return deviceList.size();
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
        return deviceList.size();
    }

    @Override
    public BTDevice getItem(int position) {
        return  deviceList.get(mKeys[position]);
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
