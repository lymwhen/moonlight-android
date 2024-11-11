package com.limelight.utils;

public class StringUtils {

    private static String[] BITRATE_UNITS = new String[]{"", "K", "M", "G"};

    public static String byteps2BpsStr(long byteps) {
        return bps2BpsStr(byteps * 8);
    }

    public static String bps2BpsStr(long byteps) {
        float cur = byteps;
        int unitIndex = 0;
        while(cur >= 1000 && unitIndex < BITRATE_UNITS.length - 1) {
            cur = cur / 1000;
            unitIndex++;
        }
        return String.format((cur >= 10 || cur == 0 ? "%.0f" : "%.1f") + " %sbps", cur, BITRATE_UNITS[unitIndex]);
    }
}
