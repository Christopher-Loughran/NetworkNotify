package com.hapax.datanotify;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

public class PermissionsManager {


    public static final String[] REQUIRED_PERMISSIONS = new String[]{android.Manifest.permission.READ_PHONE_STATE, android.Manifest.permission.ACCESS_NETWORK_STATE};


    //check a group of permissions are granted
    public static boolean checkPermissions(Context context, String[] permissions){
        for(String permission : permissions){
            if(context.checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED){
                return false;
            }
        }
        return true;
    }


    public static boolean hasAllPermissionsGranted(@NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }


}
