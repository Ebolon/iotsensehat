package de.justif.iotsensehat;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorManager.DynamicSensorCallback;
import android.content.Intent;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private SensorManager mSensorManager;
    private DynamicSensorCallback mDynamicSensorCallback = new DynamicSensorCallback() {
        @Override
        public void onDynamicSensorConnected(Sensor sensor) {

            Log.i(TAG, sensor.getStringType());

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Starting MainActivity");
        super.onCreate(savedInstanceState);
        startSenseHatService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSenseHatService();
    }

    private void startSenseHatService() {
        this.startService(new Intent(this, SenseHatService.class));
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManager.registerDynamicSensorCallback(mDynamicSensorCallback);
    }

    private void stopSenseHatService() {
        this.stopService(new Intent(this, SenseHatService.class));
        mSensorManager.unregisterDynamicSensorCallback(mDynamicSensorCallback);

    }


}
