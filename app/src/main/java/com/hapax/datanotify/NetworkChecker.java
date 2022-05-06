package com.hapax.datanotify;

import static android.telephony.TelephonyManager.NETWORK_TYPE_1xRTT;
import static android.telephony.TelephonyManager.NETWORK_TYPE_CDMA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EDGE;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_0;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_A;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_B;
import static android.telephony.TelephonyManager.NETWORK_TYPE_GPRS;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSPA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP;
import static android.telephony.TelephonyManager.NETWORK_TYPE_IDEN;
import static android.telephony.TelephonyManager.NETWORK_TYPE_LTE;
import static android.telephony.TelephonyManager.NETWORK_TYPE_NR;
import static android.telephony.TelephonyManager.NETWORK_TYPE_UMTS;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.telephony.TelephonyManager;

import androidx.core.app.ActivityCompat;

import java.lang.reflect.Method;

public class NetworkChecker extends Service {

    private static final String TAG = "DataChecker";

    public static final String MINIMUM_DATA_LEVEL = "MINIMUM_DATA_LEVEL";
    public static final String OR_BETTER = "OR_BETTER";

    private int minDataLevel = 4;
    private boolean orBetter = true;

    private boolean runCheckThread;

    Thread thread;
    ConnectivityManager connectivityManager;
    Method getMobileDataEnabled;

    public static MainActivityCallbacks mainActivityCallbacks;
    public interface MainActivityCallbacks {
        void onStarted();
        void onStopped();
    }

    public static QuickTileCallbacks quickTileCallbacks;
    public interface QuickTileCallbacks {
        void onStarted();
        void onStopped();
    }


    public NetworkChecker() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class cmClass = Class.forName(connectivityManager.getClass().getName());
            getMobileDataEnabled = cmClass.getDeclaredMethod("getMobileDataEnabled");
            getMobileDataEnabled.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        minDataLevel = intent.getIntExtra(MINIMUM_DATA_LEVEL, 4);
        orBetter = intent.getBooleanExtra(OR_BETTER, true);

        runCheckThread = false; // stop thread in case it's running
        thread = new Thread() {
            @Override
            public void run() {
                checkData();
            }
        };
        thread.start();

        String serviceNotificationText = getString(R.string.service_notification_prefix);
        if(minDataLevel > 0){
            serviceNotificationText += minDataLevel + "G";
            if(orBetter){
                serviceNotificationText += " " + getString(R.string.or_better);
            }
        }
        else{
            serviceNotificationText += getString(R.string.no_mobile_data);
        }

        onStarted();

        startForeground(NotificationsManager.SERVICE_NOTIFICATION_ID, NotificationsManager.getServiceNotification(this, serviceNotificationText));
        return START_REDELIVER_INTENT;
    }


    public void checkData() {

        runCheckThread = true;
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        while (runCheckThread) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if(checkMobileDataEnabled()) {
                switch (telephonyManager.getDataNetworkType()) {
                    case NETWORK_TYPE_EDGE:
                    case NETWORK_TYPE_GPRS:
                    case NETWORK_TYPE_CDMA:
                    case NETWORK_TYPE_IDEN:
                    case NETWORK_TYPE_1xRTT:
                        if (minDataLevel == 2 | (orBetter & minDataLevel < 2 & minDataLevel > 0)) {
                            success(getString(R.string.you_have) + "2G");
                        }
                        break;
                    case NETWORK_TYPE_UMTS:
                    case NETWORK_TYPE_HSDPA:
                    case NETWORK_TYPE_HSPA:
                    case NETWORK_TYPE_HSPAP:
                    case NETWORK_TYPE_EVDO_0:
                    case NETWORK_TYPE_EVDO_A:
                    case NETWORK_TYPE_EVDO_B:
                        if (minDataLevel == 3 | (orBetter & minDataLevel < 3 & minDataLevel > 0)) {
                            success(getString(R.string.you_have) + "3G");
                        }
                        break;
                    case NETWORK_TYPE_LTE:
                        if (minDataLevel == 4 | (orBetter & minDataLevel < 4 & minDataLevel > 0)) {
                            success(getString(R.string.you_have) + "4G");
                        }
                        break;
                    case NETWORK_TYPE_NR:
                        if (minDataLevel == 5 | (orBetter & minDataLevel < 5 & minDataLevel > 0)) {
                            success(getString(R.string.you_have) + "5G");
                        }
                        break;
                    default:
                        if (minDataLevel == 0){
                            success(getString(R.string.you_have) + getString(R.string.no_mobile_data));
                        }
                }
            }
            else{
                if (minDataLevel == 0){
                    success(getString(R.string.you_have) + getString(R.string.no_mobile_data));
                }
            }
            try{
                Thread.sleep(100);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    //The requested network settings were found
    public void success(String notificationText){
        NotificationsManager.sendReminderNotification(this, notificationText);
        runCheckThread = false;
        stopSelf();
    }


    public boolean checkMobileDataEnabled(){
        boolean mobileDataEnabled = true;
        try {
            mobileDataEnabled = (Boolean)getMobileDataEnabled.invoke(connectivityManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mobileDataEnabled;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        runCheckThread = false;
        onStopped();
    }


    public void onStarted(){
        Intent intent = new Intent(this, QuickTile.class); //start this service to create the quickTileCallbacks, that way the MainActivity button, QuickTile and service are all synchronised
        startService(intent);

        try{
            mainActivityCallbacks.onStarted();
        }
        catch (NullPointerException npe){
        }
        catch (Exception e){
            e.printStackTrace();
        }

        try{
            quickTileCallbacks.onStarted();
        }
        catch (NullPointerException npe){
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void onStopped(){
        try{
            mainActivityCallbacks.onStopped();
        }
        catch (NullPointerException npe){
        }
        catch (Exception e){
            e.printStackTrace();
        }

        try{
            quickTileCallbacks.onStopped();
        }
        catch (NullPointerException npe){
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}