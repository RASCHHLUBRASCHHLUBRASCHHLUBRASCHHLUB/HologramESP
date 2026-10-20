package com.hologram.esp.freefire;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import rikka.shizuku.Shizuku;

public class ShizukuReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Shizuku.pingBinder()) {
            Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
                if (grantResult == 0) {
                    // Permissão concedida
                }
            });
        }
    }
}
