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
import android.util.Log;

import java.lang.reflect.Field;

public class Globals {
    public static String LANGUAGE = "ro";

    public static String getStringInLocale(String stringName, Context context) {
        String name = stringName+"_"+LANGUAGE;
        String value = null;
        try {
            Field field = R.string.class.getDeclaredField(name);
            int id = field.getInt(field);
            if(id != 0) {
                value = context.getString(id);
            }
            else {
                Log.e("LANGUAGE:","no field in class R.string with the name "+name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }
}
