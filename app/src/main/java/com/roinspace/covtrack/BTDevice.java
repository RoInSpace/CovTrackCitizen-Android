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
import android.location.Location;

import java.util.Date;

public class BTDevice {
    private static final int RSSI_MAX = -50;
    private final static int RSSI_MIN = -80;
    private static int SESSION_LIFETIME = 120;

    private String  address;
    private int     maxRSSI;
    private Date    firstContactDate;
    private Date    startCurrentSessionDate;
    private Date    stopCurrentSessionDate;
    private Location startCurrentSessionLocation;
    private Location stopCurrentSessionLocation;

    private int     currentSessionDuration;
    private Date    startMaximumSessionDate;
    private Date    stopMaximumSessionDate;
    private Location startMaximumSessionLocation;
    private Location stopMaximumSessionLocation;
    private int     maximumSessionDuration = 0;
    private short   status;

    public static final short  IS_ACTIVE    = 1;
    public static final short  IS_OFF       = 2;


    public BTDevice(String address) {
        this.address = new String(address);
    }

    public BTDevice(BTDevice bt, Date lc, Location ll, int current_rssi) {
        this.clone(bt);

        if (bt.maxRSSI < current_rssi)
            this.maxRSSI = current_rssi;
        else
            this.maxRSSI = bt.maxRSSI;

        this.stopCurrentSessionLocation = new Location(ll);
        this.stopCurrentSessionDate = new Date(lc.getTime());
    }

    public BTDevice(String address,  Date date, Location lastKnownLocation, int rssi) {
        this.firstContactDate = new Date(date.getTime());
        this.stopCurrentSessionDate = new Date(date.getTime());
        this.stopCurrentSessionLocation = new Location(lastKnownLocation);
        this.maxRSSI = rssi;
        this.address = new String(address);
        this.startCurrentSessionDate = new Date(date.getTime());
        this.startCurrentSessionLocation = new Location(lastKnownLocation);

    }

    private void clone(BTDevice bt) {
        if(bt == null)
            return;

        this.address = new String(bt.address);
        this.maxRSSI = bt.maxRSSI;

        this.stopCurrentSessionDate     = new Date(bt.stopCurrentSessionDate.getTime());
        this.stopCurrentSessionLocation = new Location(bt.stopCurrentSessionLocation);

        this.startCurrentSessionDate    = new Date(bt.startCurrentSessionDate.getTime());
        this.startCurrentSessionLocation = new Location(bt.startCurrentSessionLocation);

    }

    public void updateDevice(BTDevice bt)
    {

        long timeout = (new Date().getTime() - this.stopCurrentSessionDate.getTime())/1000;
        if(timeout > SESSION_LIFETIME)
        {
            this.startCurrentSessionDate = new Date();
            this.startCurrentSessionLocation = bt.stopCurrentSessionLocation;
            this.currentSessionDuration = 0;
        }
        else
        {
            this.startCurrentSessionDate = new Date(bt.startCurrentSessionDate.getTime());
            this.startCurrentSessionLocation = new Location(bt.startCurrentSessionLocation);
        }

        this.stopCurrentSessionDate = new Date(bt.stopCurrentSessionDate.getTime());
        this.stopCurrentSessionLocation = new Location(bt.stopCurrentSessionLocation);
        this.maxRSSI = bt.maxRSSI;
        this.address = bt.address;

        updateDevice();
    }

    public void updateDevice()
    {
        this.currentSessionDuration = (int) getCurrentSessionDuration();

        if(currentSessionDuration > maximumSessionDuration)
        {
            this.maximumSessionDuration = currentSessionDuration;
            this.startMaximumSessionDate = this.startCurrentSessionDate;
            this.stopMaximumSessionDate = this.stopCurrentSessionDate;
            this.stopMaximumSessionLocation = this.stopCurrentSessionLocation;
            this.startMaximumSessionLocation = this.startCurrentSessionLocation;
        }

    }


    public String Print()
    {
        String text =
                "MAC address" + getAddress() + "\n" +
                "First detectd at: " + getAddress() + "\n" +
                "Start session date: " + getStartCurrentSessionDate().toString() +"\n" +
                "Stop session date: " + getStopCurrentSessionDate().toString() +"\n" +
                "Start session location: " + getStartCurrentSessionLocation().getLatitude() + " " + getStartCurrentSessionLocation().getLongitude() + "\n" +
                "Stop session location: " + getStopCurrentSessionLocation().getLatitude() + " " + getStopCurrentSessionLocation().getLongitude() +"\n" +
                "Current session time: " + getCurrentSessionDuration() + " seconds\n";

        if(maximumSessionDuration > 0)
        {
            text +=
                "Start MAX session date: " + startMaximumSessionDate.toString() +"\n" +
                "Stop MAX session date: " + stopMaximumSessionDate.toString() +"\n" +
                "Start MAX session location: " + startMaximumSessionLocation.getLatitude() + " " + startMaximumSessionLocation.getLongitude() + "\n" +
                "Stop MAX session location: " + stopMaximumSessionLocation.getLatitude() + " " + stopMaximumSessionLocation.getLongitude() +"\n" +
                "MAX session time " + maximumSessionDuration + " seconds\n";
        }

        return text;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = new String(address);
    }

    public int getMaxRSSI() {
        return maxRSSI;
    }

    public void setMaxRSSI(int maxRSSI) {
        this.maxRSSI = maxRSSI;
    }

    public long getCurrentSessionDuration() {
        return (stopCurrentSessionDate.getTime()- startCurrentSessionDate.getTime())/1000;
    }

    public Date getFirstContactDate() {
        return firstContactDate;
    }

    public Date getStartCurrentSessionDate() {
        return startCurrentSessionDate;
    }

    public void setStartCurrentSessionDate(Date startCurrentSessionDate) {
        this.startCurrentSessionDate = new Date(startCurrentSessionDate.getTime());
    }

    public Location getStopCurrentSessionLocation() {
        return stopCurrentSessionLocation;
    }

    public Location getStartCurrentSessionLocation(){return startCurrentSessionLocation;}

    public void setStopCurrentSessionLocation(Location location) {
        this.stopCurrentSessionLocation = new Location(location);
    }

    public Date getStopCurrentSessionDate() {
        return stopCurrentSessionDate;
    }

    public void setStopCurrentSessionDate(Date stopCurrentSessionDate) {
        this.stopCurrentSessionDate = new Date(stopCurrentSessionDate.getTime());
    }

    public Date getStartMaximumSessionDate() {
        return startMaximumSessionDate;
    }

    public Date getStopMaximumSessionDate() {
        return stopMaximumSessionDate;
    }

    public Location getStartMaximumSessionLocation() {
        return startMaximumSessionLocation;
    }

    public Location getStopMaximumSessionLocation() {
        return stopMaximumSessionLocation;
    }

    public int getMaximumSessionDuration() {
        return maximumSessionDuration;
    }

    public String roughDistance(Context context) {
        if (maxRSSI > RSSI_MAX ) return Globals.getStringInLocale("text_distance_very_close", context);
        if (maxRSSI > RSSI_MIN ) return Globals.getStringInLocale("text_distance_close", context);
        else return Globals.getStringInLocale("text_distance_far", context);
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }
}
