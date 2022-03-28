package com.example.datanotify;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

public class QuickTile extends TileService {

    private static final String TAG = "QuickTile";

    private static final String TILE_PREFERTENCES = "TILE_PREFERTENCES";
    private static final String ACTIVE_KEY = "ACTIVE_KEY";

    //data checker params
    private int minDataLevel = 3;
    private boolean orBetter = false;


    @Override
    public void onCreate(){
        DataChecker.quickTileCallbacks = new DataChecker.QuickTileCallbacks() {
            @Override
            public void onStarted() {
                setActive(true);
            }

            @Override
            public void onStopped() {
                setActive(false);
            }
        };
    }


    @Override
    public void onClick() {
        super.onClick();

        if(!getActive()){ //service is not turned on
            startDataCheckerService();
        }
        else{
            stopDataCheckerService();
        }
    }


    @Override
    public void onTileAdded() {
        super.onTileAdded();
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    public void startDataCheckerService(){
        if (PermissionsManager.checkPermissions(this, PermissionsManager.REQUIRED_PERMISSIONS)) {
            Intent intent = new Intent(this, DataChecker.class);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String dataLevel = prefs.getString(getString(R.string.min_data_level_key), "4G");
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

            orBetter = prefs.getBoolean(getString(R.string.or_better_key), true);

            intent.putExtra(DataChecker.MINIMUM_DATA_LEVEL, minDataLevel);
            intent.putExtra(DataChecker.OR_BETTER, orBetter);


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
                setActive(true);
            }
            else{
                startService(intent);
                setActive(true);
            }
        } else {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
    }


    public void stopDataCheckerService(){
        Intent intent = new Intent(this, DataChecker.class);
        stopService(intent);
        setActive(false);
    }


    public boolean getActive() {
        SharedPreferences sharedPreferences = getSharedPreferences(TILE_PREFERTENCES, MODE_PRIVATE);
        boolean inKioskMode = sharedPreferences.getBoolean(ACTIVE_KEY, false); //false as default
        return inKioskMode;
    }


    public void setActive(boolean active)
    {
        SharedPreferences.Editor sharedPreferencesEditor = getSharedPreferences(TILE_PREFERTENCES, MODE_PRIVATE).edit();
        sharedPreferencesEditor.putBoolean(ACTIVE_KEY, active);
        sharedPreferencesEditor.apply();
        setTileActive(active);
    }


    public void setTileActive(boolean active){
        Tile tile = getQsTile();
        if(active){
            tile.setState(Tile.STATE_ACTIVE);
        }
        else{
            tile.setState(Tile.STATE_INACTIVE);
        }
        tile.updateTile();
    }
}
