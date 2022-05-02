package com.hapax.datanotify;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

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
        NetworkChecker.quickTileCallbacks = new NetworkChecker.QuickTileCallbacks() {
            @Override
            public void onStarted() {
                setActive(true);
            } //the network checker service has started

            @Override
            public void onStopped() {
                setActive(false);
            } //the network cheker service has stopped
        };
    }


    @Override
    public void onClick() { //user clicks the quick tile
        super.onClick();

        if(!getActive()){ //service is not turned on
            if(PermissionsManager.checkPermissions(this, PermissionsManager.REQUIRED_PERMISSIONS)){
                startDataCheckerService();
            }
            else{
                //user has not granted the required permissions, send them to the settings activity so they can grant them.
                startSettingsActivity();
            }
        }
        else{
            stopDataCheckerService();
        }
    }


    @Override
    public void onTileAdded() {
        super.onTileAdded();
        setTileActive(getActive());
        //start settings activity so the user can accept the required permissions and set their preferred default settings
        startSettingsActivity();
    }


    public void startSettingsActivity(){
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    public void startDataCheckerService(){
        if (PermissionsManager.checkPermissions(this, PermissionsManager.REQUIRED_PERMISSIONS)) {
            Intent intent = new Intent(this, NetworkChecker.class);

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

            intent.putExtra(NetworkChecker.MINIMUM_DATA_LEVEL, minDataLevel);
            intent.putExtra(NetworkChecker.OR_BETTER, orBetter);


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
        Intent intent = new Intent(this, NetworkChecker.class);
        stopService(intent);
        setActive(false);
    }


    public boolean getActive() {
        SharedPreferences sharedPreferences = getSharedPreferences(TILE_PREFERTENCES, MODE_PRIVATE);
        boolean serviceActive = sharedPreferences.getBoolean(ACTIVE_KEY, false); //false as default
        return serviceActive;
    }


    public void setActive(boolean active)
    {
        SharedPreferences.Editor sharedPreferencesEditor = getSharedPreferences(TILE_PREFERTENCES, MODE_PRIVATE).edit();
        sharedPreferencesEditor.putBoolean(ACTIVE_KEY, active);
        sharedPreferencesEditor.apply();
        setTileActive(active);
    }


    public void setTileActive(boolean active){
        try{
            Tile tile = getQsTile();
            if(active){
                tile.setState(Tile.STATE_ACTIVE);
            }
            else{
                tile.setState(Tile.STATE_INACTIVE);
            }
            tile.updateTile();
        }
        catch (NullPointerException e){
            e.printStackTrace();
        }

    }
}
