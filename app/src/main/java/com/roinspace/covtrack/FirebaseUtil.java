package com.roinspace.covtrack;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

class Patient {

    public String mac_addr;

    public Patient() {

    }

}

public class FirebaseUtil {

    private static  String TAG = "FIREBASE";
    private static  String eventId = "testevent";
    static private FirebaseAuth mAuth;

    public static FirebaseDatabase remoteDatabase = FirebaseDatabase.getInstance("https://covid-c1101.firebaseio.com/");

    public interface DatabasePatientsListener {
        public void onReceivePatientsSuccess(Vector<String> patients);
        public void onReceivePatientsFailed();
    }

    public static  void addBluetoothAddress(String macAddress) {

        DatabaseReference rootRef = remoteDatabase.getReference();
        DatabaseReference patientsRef = rootRef.child("patients").push();
        String key = "mac_addr";
        Map<String, Object> map = new HashMap<>();
        map.put(key, macAddress);
        patientsRef.updateChildren(map);
    }

    public static void getPatients(final DatabasePatientsListener callback)
    {
        // Get a reference to our posts
        DatabaseReference rootRef = remoteDatabase.getReference();
        DatabaseReference patientsRef = rootRef.child("patients");

        // Attach a listener to read the data at our posts reference
        patientsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Vector<String> patients = new Vector<>();

                if (dataSnapshot.exists()) {

                    Map<String, Object> patientsList = (Map<String, Object>) dataSnapshot.getValue();
                    String[] keys = patientsList.keySet().toArray(new String[patientsList.size()]);

                    if (keys.length > 0) {

                        for (int i = 0; i < keys.length; i++) {

                            Map<String, Object> patientsObj = new HashMap<>((Map<String, Object>) patientsList.get(keys[i]));
                            String jsonString = new Gson().toJson(patientsObj);
                            Patient patientModel = new Gson().fromJson(jsonString, Patient.class);
                            patients.add(patientModel.mac_addr);

                        }
                    }
                }
                System.out.println(patients);
                callback.onReceivePatientsSuccess(patients);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onReceivePatientsFailed();
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }
}
