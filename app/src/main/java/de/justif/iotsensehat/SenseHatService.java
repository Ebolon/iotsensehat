package de.justif.iotsensehat;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.google.android.things.contrib.driver.sensehat.Joystick;
import com.google.android.things.contrib.driver.sensehat.LedMatrix;
import com.google.android.things.contrib.driver.sensehat.SenseHat;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import de.justif.iotsensehat.cloud.CloudPublisherService;

import static com.google.android.things.contrib.driver.sensehat.Joystick.*;

/**
 * To use this service, start it from your component (like an activity):
 * <pre>{@code
 * this.startService(new Intent(this, TemperaturePressureService.class))
 * }</pre>
 */
public class SenseHatService extends Service {
    private static final String TAG = SenseHatService.class.getSimpleName();
    private static final long SENSOR_READ_INTERVAL_MS = TimeUnit.SECONDS.toMillis(20);
    public static final String SENSOR_TYPE_JOYSTICK = "joystick";
    /**
     * Cutoff to consider a timestamp as valid. Some boards might take some time to update
     * their network time on the first time they boot, and we don't want to publish sensor data
     * with timestamps that we know are invalid. Sensor readings will be ignored until the
     * board's time (System.currentTimeMillis) is more recent than this constant.
     */
    private static final long INITIAL_VALID_TIMESTAMP;
    static {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2016, 1, 1);
        INITIAL_VALID_TIMESTAMP = calendar.getTimeInMillis();
    }
    private CloudPublisherService mPublishService;
    private SenseHat mSenseHat;
    private Joystick mJoystick;
    private LedMatrix mLedMatrix;

    @Override
    public void onCreate() {
        Log.i(TAG, "Creating SenseHatService");
        initializeServiceIfNeeded();
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
            mJoystick.setOnButtonEventListener(new OnButtonEventListener() {
                @Override
                public void onButtonEvent(int key, boolean pressed) {
                    Log.i(TAG, "key: " + key + "pressed" + pressed);
                    collectSensorOnChange(SENSOR_TYPE_JOYSTICK, pressed ? 1 : 0);
                }});
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
                mJoystick = null;
                mLedMatrix = null;
                mSenseHat = null;
            }
        }
        // unbind from Cloud Publisher service.
        if (mPublishService != null) {
            unbindService(mServiceConnection);
        }
    }

    private void collectSensorOnChange(String type, float sensorReading) {
        if (mPublishService != null) {
            Log.d(TAG, "On change " + type + ": " + sensorReading);
            long now = System.currentTimeMillis();
            if (now >= INITIAL_VALID_TIMESTAMP) {
                mPublishService.logSensorDataOnChange(new SensorData(now, type, sensorReading));
            } else {
                Log.i(TAG, "Ignoring sensor readings because timestamp is invalid. " +
                        "Please, set the device's date/time");
            }
        }
    }

    private void initializeServiceIfNeeded() {
        if (mPublishService == null) {
            try {
                // Bind to the service
                Intent intent = new Intent(this, CloudPublisherService.class);
                bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
            } catch (Throwable t) {
                Log.e(TAG, "Could not connect to the service, will try again later", t);
            }
        }
    }

    /**
     * Callback for service binding, passed to bindService()
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            CloudPublisherService.LocalBinder binder = (CloudPublisherService.LocalBinder) service;
            mPublishService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mPublishService = null;
        }
    };

}
