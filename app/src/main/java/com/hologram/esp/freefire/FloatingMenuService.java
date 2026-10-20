package com.hologram.esp.freefire;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class FloatingMenuService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private View minimizedView;
    private LinearLayout menuLayout;
    private ESPOverlayView espView;
    private boolean isMenuExpanded = false;
    private boolean isHologramEnabled = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, getNotification());

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        setupMinimizedView();
        setupMenuView();
        setupESPView();
    }

    private void setupMinimizedView() {
        minimizedView = new View(this);
        minimizedView.setLayoutParams(new WindowManager.LayoutParams(
                100, 100,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT));

        minimizedView.setBackgroundColor(0xCC007BFF);
        minimizedView.setOnClickListener(v -> toggleMenu());
        
        addViewWithDragSupport(minimizedView);
        minimizedView.setVisibility(View.GONE);
    }

    private void setupMenuView() {
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.floating_menu, null);
        
        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        windowManager.addView(floatingView, params);

        Button btnMinimize = floatingView.findViewById(R.id.btn_minimize);
        Switch switchHologram = floatingView.findViewById(R.id.switch_hologram);
        TextView txtStatus = floatingView.findViewById(R.id.txt_status);

        btnMinimize.setOnClickListener(v -> toggleMenu());

        switchHologram.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isHologramEnabled = isChecked;
            if (espView != null) {
                espView.setHologramEnabled(isHologramEnabled);
            }
            txtStatus.setText(isHologramEnabled ? "Holograma Azul: ON" : "Holograma Azul: OFF");
        });

        addViewWithDragSupport(floatingView);
    }

    private void setupESPView() {
        espView = new ESPOverlayView(this);
        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);

        windowManager.addView(espView, params);
    }

    private void toggleMenu() {
        isMenuExpanded = !isMenuExpanded;
        floatingView.setVisibility(isMenuExpanded ? View.VISIBLE : View.GONE);
        minimizedView.setVisibility(isMenuExpanded ? View.GONE : View.VISIBLE);
    }

    private void addViewWithDragSupport(final View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) v.getLayoutParams();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(v, params);
                        return true;
                }
                return false;
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("esp_channel", "ESP Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification getNotification() {
        return new Notification.Builder(this, "esp_channel")
                .setContentTitle("Hologram ESP")
                .setContentText("Serviço de overlay ativo")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
        if (minimizedView != null) windowManager.removeView(minimizedView);
        if (espView != null) windowManager.removeView(espView);
    }
}
