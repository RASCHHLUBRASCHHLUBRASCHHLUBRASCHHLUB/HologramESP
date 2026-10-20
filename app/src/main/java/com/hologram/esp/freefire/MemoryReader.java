package com.hologram.esp.freefire;

import android.graphics.RectF;
import android.util.Log;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class MemoryReader {

    private static final String TARGET_PACKAGE = "com.dts.freefireth";
    private long baseAddress = 0;
    private int pid = -1;
    
    private static final long OFFSET_PLAYER_LIST = 0x1A2B3C40; 
    private static final long OFFSET_PLAYER_POSITION = 0x10;
    private static final long OFFSET_PLAYER_HEALTH = 0x14;
    private static final long OFFSET_PLAYER_NAME = 0x20;
    private static final long OFFSET_VIEW_MATRIX = 0x9A8B7C60;

    private List<PlayerData> playerList = new ArrayList<>();

    public MemoryReader() {
        initShizukuMemoryAccess();
    }

    private void initShizukuMemoryAccess() {
        try {
            ShizukuRemoteProcess process = Shizuku.newProcess(new String[]{"sh", "-c", 
                    "pidof " + TARGET_PACKAGE + " && cat /proc/$(pidof " + TARGET_PACKAGE + ")/maps"}, null, null);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("libil2cpp.so") || line.contains("base.odex")) {
                    String[] parts = line.split("-");
                    baseAddress = Long.parseLong(parts[0], 16);
                    break;
                }
            }
            
            process = Shizuku.newProcess(new String[]{"pidof", TARGET_PACKAGE}, null, null);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String pidStr = reader.readLine();
            if (pidStr != null) {
                pid = Integer.parseInt(pidStr.trim());
            }
        } catch (Exception e) {
            Log.e("MemoryReader", "Erro ao iniciar Shizuku: " + e.getMessage());
        }
    }

    public void updatePlayerData() {
        if (pid == -1 || baseAddress == 0) return;

        try {
            ShizukuRemoteProcess process = Shizuku.newProcess(new String[]{
                    "su", "-c", "dd if=/proc/" + pid + "/mem bs=1 count=1024 skip=" + (baseAddress + OFFSET_PLAYER_LIST)
            }, null, null);

            DataInputStream dis = new DataInputStream(process.getInputStream());
            byte[] buffer = new byte[1024];
            dis.readFully(buffer);
            
            ByteBuffer bb = ByteBuffer.wrap(buffer);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            playerList.clear();
            
            for (int i = 0; i < 50; i++) {
                int offset = i * 0x100;
                if (offset + 0x50 > buffer.length) break;

                PlayerData player = new PlayerData();
                
                float x = bb.getFloat(offset + (int)OFFSET_PLAYER_POSITION);
                float y = bb.getFloat(offset + (int)OFFSET_PLAYER_POSITION + 4);
                float z = bb.getFloat(offset + (int)OFFSET_PLAYER_POSITION + 8);
                
                float health = bb.getFloat(offset + (int)OFFSET_PLAYER_HEALTH);
                
                if (health > 0 && health <= 100 && x != 0 && y != 0) {
                    player.isAlive = true;
                    player.distance = calculateDistance(x, y, z);
                    player.name = "Player " + i;
                    
                    player.screenBox = projectToScreen(x, y, z);
                    playerList.add(player);
                }
            }
        } catch (Exception e) {
            // Falha silenciosa
        }
    }

    private float calculateDistance(float x, float y, float z) {
        return (float) Math.sqrt(x*x + y*y + z*z);
    }

    private RectF projectToScreen(float x, float y, float z) {
        float screenX = (x * 0.5f) + 540;
        float screenY = (y * 0.5f) + 960;
        float height = 2000 / (z + 1);
        float width = height * 0.5f;

        return new RectF(screenX - width/2, screenY - height, screenX + width/2, screenY);
    }

    public List<PlayerData> getPlayerList() {
        return playerList;
    }

    public static class PlayerData {
        public boolean isAlive = false;
        public float distance = 0;
        public String name = "";
        public RectF screenBox = new RectF();
    }
}
