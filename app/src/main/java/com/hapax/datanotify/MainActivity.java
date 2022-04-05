package com.hapax.datanotify;

import androidx.annotation.NonNull;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Spinner;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private static final int PERMISSIONS_REQUEST_CODE = 2648;

    //data checker params
    private int minDataLevel = 3;
    private boolean orBetter = false;


    //UI elements
    private Spinner spinner;
    private CheckBox checkBox;
    private Button button;
    private ImageButton settingsButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationsManager.createNotificationChannel(this);

        DataChecker.mainActivityCallbacks = new DataChecker.MainActivityCallbacks() {
            @Override
            public void onStarted() {
                onDataCheckerServiceStarted();
            }
            @Override
            public void onStopped() {
                onDataCheckerServiceStopped();
            }
        };


        //setup ui elements
        spinner = (Spinner) findViewById(R.id.data_type_menu);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.data_types_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String dataLevel = (String) adapterView.getItemAtPosition(i);

                switch (dataLevel){
                    case "None":
                        minDataLevel = 0;
                        break;
                    case "2G":
                        minDataLevel = 2;
                        break;
                    case "3G":
                        minDataLevel = 3;
                        break;
                    case "4G":
                        minDataLevel = 4;
                        break;
                    case "5G":
                        minDataLevel = 5;
                        break;
                    default:
                        minDataLevel = 0;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                minDataLevel = 0;
            }
        });


        checkBox = findViewById(R.id.or_better_checkbox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                orBetter = b;
            }
        });


        button = findViewById(R.id.start_button);
        if(!isServiceRunning(DataChecker.class)){
            button.setText(R.string.start);
        }
        else{
            button.setText(R.string.stop);
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isServiceRunning(DataChecker.class)){
                    startDataCheckerService();
                }
                else{
                    stopDataCheckerService();
                }
            }
        });

        Context context = this;
        settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, SettingsActivity.class);
                startActivity(intent);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.w(TAG, "No activity found to handle " + intent.toString());
                }
            }
        });
    }


    public void startDataCheckerService(){
        if (PermissionsManager.checkPermissions(this, PermissionsManager.REQUIRED_PERMISSIONS)) {

            Intent intent = new Intent(this, DataChecker.class);
            intent.putExtra(DataChecker.MINIMUM_DATA_LEVEL, minDataLevel);
            intent.putExtra(DataChecker.OR_BETTER, orBetter);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            }
            else{
                startService(intent);
            }
        } else {
            requestPermissions(PermissionsManager.REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
        }
        if(!isServiceRunning(DataChecker.class)){ button.setText(R.string.start); }
        else{ button.setText(R.string.stop); }
    }


    public void stopDataCheckerService(){
        Intent intent = new Intent(this, DataChecker.class);
        stopService(intent);

        if(!isServiceRunning(DataChecker.class)){ button.setText(R.string.start); }
        else{ button.setText(R.string.stop); }
    }


    public void onDataCheckerServiceStopped(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setText(R.string.start);
            }
        });
    }


    private void onDataCheckerServiceStarted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setText(R.string.stop);
            }
        });
    }



    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    //if permissions were granted, try to start service again
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PERMISSIONS_REQUEST_CODE){
            if(PermissionsManager.hasAllPermissionsGranted(grantResults)){
                startDataCheckerService();
            }
        }
    }

}