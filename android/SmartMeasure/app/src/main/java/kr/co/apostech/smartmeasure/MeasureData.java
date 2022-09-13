package kr.co.apostech.smartmeasure;

import android.util.Log;

import java.io.Serializable;
import java.util.Arrays;

public class MeasureData implements Serializable {

    public static String SHARED_PREF_KEY_NAME = "SmartMeasure";
    public static String PREF_KEY_DEV_ADDR = "dev_addr";
    public static String UUID_MEASURE_SVC = "000000FF-0000-1000-8000-00805F9B34FB";
    public static String UUID_MEASURE_RESULT = "0000FF01-0000-1000-8000-00805F9B34FB";
    public static String UUID_MEASURE_CMD = "0000FF02-0000-1000-8000-00805F9B34FB";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static final int MODE_WAIT = 0;
    public static final int MODE_HEIGHT = 1;
    public static final int MODE_DISTANCE = 2;
    public static final int MODE_CONFIG = 3;

    public static String STR_MODE_WAIT = "WAIT MODE";
    public static String STR_MODE_HEIGHT = "HEIGHT MEASURE";
    public static String STR_MODE_DISTANCE = "DISTANCE MEASURE";
    public static String STR_MODE_CONFIG = "HEIGHT SETTING";

    private int mode;
    private int value;

    public MeasureData() {
    }

    public void setData(byte[] b) {
        //this.mode = ((b[3] & 0xFF) << 8) + ((b[2] & 0xFF));
        //this.value = ((b[1] & 0xFF) << 8) + (b[0] & 0xFF);
        this.value = ((b[3] & 0xFF) << 8) + ((b[2] & 0xFF));
        this.mode = ((b[1] & 0xFF) << 8) + (b[0] & 0xFF);
        Log.e("DATA","mode("+Integer.toString(this.mode)+"), value("+Integer.toString(this.value)+")");
    }

    public int getMode() {
        return this.mode;
    }
    public int getValue() {
        return this.value;
    }


}