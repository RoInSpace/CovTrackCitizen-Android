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

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


public class InfectedPatientsActivity extends AppCompatActivity {


    private ImageButton backBtn;
    private Context     context;
    private TextView    infectedDevicesTextView;
    private ListView    patientsList;

    private ArrayList<DeviceDBModel> receivedPatientsList;

    public  final static String RECEIVE_PATIENT_LIST = "RECEIVE_PATIENT_LIST";

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_infected_patients);
        context = this;

        Bundle extras = getIntent().getExtras();

        infectedDevicesTextView = findViewById(R.id.textView_infected_devices);
        patientsList            = findViewById(R.id.listView);
        backBtn                 = findViewById(R.id.verifyBtn);

        if(Globals.LANGUAGE.compareTo("ro") == 0)
        {
            backBtn.setImageResource(R.drawable.main_menu_btn_selector_ro);

        }
        else
        {
            backBtn.setImageResource(R.drawable.main_menu_btn_selector_en);

        }

        ObjectAnimator colorAnim = ObjectAnimator.ofInt(infectedDevicesTextView, "textColor",
                Color.RED, Color.GREEN);
        colorAnim.setEvaluator(new ArgbEvaluator());
        colorAnim.setDuration(1000);
        colorAnim.setRepeatMode(Animation.REVERSE);
        colorAnim.setRepeatCount(Animation.INFINITE);
        colorAnim.start();

        infectedDevicesTextView.setText(Globals.getStringInLocale("text_infected_devices_brief",this));


        if (extras != null) {
            receivedPatientsList = extras.getParcelableArrayList(RECEIVE_PATIENT_LIST);
            patientsList.setAdapter(new DeviceDBModelAdapter(context, receivedPatientsList));
        }

        backBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(context, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        finish();
        startActivity(new Intent(this, MainActivity.class));
    }

}
