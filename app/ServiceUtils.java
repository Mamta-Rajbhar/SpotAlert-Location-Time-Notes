package com.example.location; // Ensure this matches your package name

import android.app.ActivityManager;
import android.content.Context;

public class ServiceUtils {

    /**
     * Check if a service is running.
     *
     * @param context      The application context.
     * @param serviceClass The class of the service to check.
     * @return True if the service is running, false otherwise.
     */
    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }
}