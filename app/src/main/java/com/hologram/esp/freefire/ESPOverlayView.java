package com.hologram.esp.freefire;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

public class ESPOverlayView extends View {

    private boolean isHologramEnabled = false;
    private Paint boxPaint;
    private Paint textPaint;
    private MemoryReader memoryReader;
    private Handler handler;
    private Runnable updateRunnable;

    private static final int HOLOGRAM_COLOR = 0xAA00BFFF;
    private static final int HOLOGRAM_GLOW = 0x5500FFFF;

    public ESPOverlayView(Context context) {
        super(context);
        init();
    }

    private void init() {
        boxPaint = new Paint();
        boxPaint.setColor(HOLOGRAM_COLOR);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4f);
        boxPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.CYAN);
        textPaint.setTextSize(30f);
        textPaint.setAntiAlias(true);
        textPaint.setShadowLayer(5f, 0, 0, HOLOGRAM_GLOW);

        memoryReader = new MemoryReader();
        handler = new Handler(Looper.getMainLooper());
        
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isHologramEnabled) {
                    memoryReader.updatePlayerData();
                    invalidate();
                }
                handler.postDelayed(this, 16);
            }
        };
        handler.post(updateRunnable);
    }

    public void setHologramEnabled(boolean enabled) {
        isHologramEnabled = enabled;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isHologramEnabled) return;

        boxPaint.setShadowLayer(10f, 0, 0, HOLOGRAM_GLOW);
        
        for (MemoryReader.PlayerData player : memoryReader.getPlayerList()) {
            if (player.isAlive && player.distance > 0) {
                RectF box = player.screenBox;
                
                Paint fillPaint = new Paint();
                fillPaint.setColor(HOLOGRAM_GLOW);
                fillPaint.setStyle(Paint.Style.FILL);
                canvas.drawRect(box, fillPaint);
                
                canvas.drawRect(box, boxPaint);
                
                canvas.drawLine(box.centerX(), box.top, box.centerX(), box.bottom, boxPaint);
                
                canvas.drawText(player.name + " [" + (int)player.distance + "m]", 
                                box.left, box.top - 10, textPaint);
            }
        }
    }
}
