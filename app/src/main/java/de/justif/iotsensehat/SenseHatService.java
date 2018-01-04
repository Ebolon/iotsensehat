package de.justif.iotsensehat;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.android.things.contrib.driver.bmx280.Bmx280SensorDriver;
import com.google.android.things.contrib.driver.sensehat.Joystick;
import com.google.android.things.contrib.driver.sensehat.LedMatrix;
import com.google.android.things.contrib.driver.sensehat.SenseHat;

import java.io.IOException;

/**
 * To use this service, start it from your component (like an activity):
 * <pre>{@code
 * this.startService(new Intent(this, TemperaturePressureService.class))
 * }</pre>
 */
public class SenseHatService extends Service {
    private static final String TAG = SenseHatService.class.getSimpleName();

    private SenseHat mSenseHat;
    private Joystick mJoystick;
    private JoystickEventListener mJoystickEventListener;
    private LedMatrix mLedMatrix;

    @Override
    public void onCreate() {
        Log.i(TAG, "Creating SenseHatService");
        setupSenseHat();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroySenseHat();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    private void setupSenseHat() {
        try {
            Log.i(TAG, "Setup SenseHatService");
            mSenseHat = new SenseHat();
            mJoystick = mSenseHat.openJoystick();
            mJoystickEventListener = new JoystickEventListener();
            mJoystick.setOnButtonEventListener(mJoystickEventListener);
            mLedMatrix = mSenseHat.openDisplay();
            mLedMatrix.draw(0);
        } catch (IOException e) {
            Log.e(TAG, "Error configuring sensor", e);
        }
    }

    private void destroySenseHat() {
        if (mSenseHat != null) {
            try {
                mSenseHat.closeJoystick();
                mSenseHat.close();
                mSenseHat.closeDisplay();
            } catch (IOException e) {
                Log.e(TAG, "Error closing sensor", e);
            } finally {
                mJoystickEventListener = null;
                mJoystick = null;
                mLedMatrix = null;
                mSenseHat = null;
            }
        }
    }

}
