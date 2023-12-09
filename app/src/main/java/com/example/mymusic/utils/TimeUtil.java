package com.example.mymusic.utils;

public class TimeUtil {
    /**
     * 毫秒转为00:00格式
     * @param timeMills
     * @return
     */

    public static String millToTimeFormat(int timeMills) {
        // 3600ms
        int second = timeMills / 1000;
        int minute = second / 60;
        int lastSecond = second % 60;

        String strSecond = "";
        if (lastSecond < 10) {
            strSecond = "0" + lastSecond;
        } else {
            strSecond = lastSecond + "";
        }

        String strMinute = "";
        if (minute < 10) {
            strMinute = "0" + minute;
        } else {
            strMinute = minute + "";
        }
        return strMinute + ":" + strSecond;
    }


}
