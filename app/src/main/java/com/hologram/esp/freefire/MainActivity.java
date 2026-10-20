package com.hologram.esp.freefire;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST);
            Toast.makeText(this, "Conceda a permissão de sobrepor a outros apps", Toast.LENGTH_LONG).show();
            return;
        }

        checkShizukuPermission();
        startFloatingService();
        finish();
    }

    private void checkShizukuPermission() {
        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() != 0) {
                Shizuku.requestPermission(1);
            }
        } else {
            Toast.makeText(this, "Shizuku não está rodando. Inicie o Shizuku via ADB ou Root.", Toast.LENGTH_LONG).show();
        }
    }

    private void startFloatingService() {
        Intent serviceIntent = new Intent(this, FloatingMenuService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            if (Settings.canDrawOverlays(this)) {
                checkShizukuPermission();
                startFloatingService();
                finish();
            }
        }
    }
}
