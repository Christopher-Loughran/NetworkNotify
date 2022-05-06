package com.hapax.datanotify;

import android.app.ActivityManager;
import android.content.Context;
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
    private int minDataLevel = 4;
    private boolean orBetter = true;


    @Override
    public void onCreate(){

        NetworkChecker.quickTileCallbacks = new NetworkChecker.QuickTileCallbacks() {
            @Override
            public void onStarted() {
                Log.w(TAG, "QuickTile onStarted callback");
                setTileActive(true);
            } //the network checker service has started

            @Override
            public void onStopped() {
                setTileActive(false);
                Log.w(TAG, "QuickTile onStopped callback");
            } //the network checker service has stopped
        };

        setTileActive(isServiceRunning(NetworkChecker.class));

    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        setTileActive(isServiceRunning(NetworkChecker.class));
    }

    @Override
    public void onClick() { //user clicks the quick tile
        super.onClick();

        if(!isServiceRunning(NetworkChecker.class)){ //service is not turned on
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
        setTileActive(isServiceRunning(NetworkChecker.class));
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
                setTileActive(true);
            }
            else{
                startService(intent);
                setTileActive(true);
            }
        } else {

            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
    }


    public void stopDataCheckerService(){
        Intent intent = new Intent(this, NetworkChecker.class);
        stopService(intent);
        setTileActive(false);
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


    public void setTileActive(boolean active){
        try{
            Tile tile = getQsTile();
            if(active){
                tile.setState(Tile.STATE_ACTIVE);
                Log.w(TAG, "Setting tile to ACTIVE");
            }
            else{
                tile.setState(Tile.STATE_INACTIVE);
                Log.w(TAG, "Setting tile to INACTIVE");
            }
            tile.updateTile();
        }
        catch (NullPointerException e){
            e.printStackTrace();
        }
    }
}
