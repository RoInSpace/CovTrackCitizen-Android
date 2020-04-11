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

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

public class DeviceDBModel implements Parcelable {
        public String   macAddress              = "";
        public String   sessionStartDate        = "";
        public String   sessionStopDate         = "";
        public Location sessionStartLocation    = new Location("");
        public Location sessionStopLocation     = new Location("");
        public int      sessionDuration         = 0;

        @Override
        public int describeContents() {
                return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {

                dest.writeString(macAddress);
                dest.writeString(sessionStartDate);
                dest.writeString(sessionStopDate);
                sessionStartLocation.writeToParcel(dest, flags);
                sessionStopLocation.writeToParcel(dest, flags);
                dest.writeInt(sessionDuration);
        }

        public DeviceDBModel() { }

        public DeviceDBModel(Parcel in) {

                this.macAddress                 = in.readString();
                this.sessionStartDate           = in.readString();
                this.sessionStopDate            = in.readString();
                this.sessionStartLocation       = Location.CREATOR.createFromParcel(in);
                this.sessionStopLocation        = Location.CREATOR.createFromParcel(in);
                this.sessionDuration            = in.readInt();
        }

        public void readFromParcel(Parcel in) {
                this.macAddress                 = in.readString();
                this.sessionStartDate           = in.readString();
                this.sessionStopDate            = in.readString();
                this.sessionStartLocation       = Location.CREATOR.createFromParcel(in);
                this.sessionStopLocation        = Location.CREATOR.createFromParcel(in);
                this.sessionDuration            = in.readInt();
        }

        public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {

                public DeviceDBModel createFromParcel(Parcel in) {
                        return  new DeviceDBModel(in);
                }

                public DeviceDBModel[] newArray(int size) {
                        return new DeviceDBModel[size];
                }
        };
}
