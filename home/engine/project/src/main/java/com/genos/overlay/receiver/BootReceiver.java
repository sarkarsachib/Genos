package com.genos.overlay.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Boot receiver to start GENOS services after device boot
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "Received broadcast: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            
            Log.i(TAG, "Device boot completed, starting GENOS services");
            
            // Here you could start your services automatically if needed
            // Intent overlayIntent = new Intent(context, OverlayService.class);
            // context.startService(overlayIntent);
            
            // Intent accessibilityIntent = new Intent(context, InputEmulatorService.class);
            // context.startService(accessibilityIntent);
        }
    }
}