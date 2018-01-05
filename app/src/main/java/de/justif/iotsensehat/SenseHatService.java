package de.justif.iotsensehat;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.google.android.things.contrib.driver.hts221.Hts221;
import com.google.android.things.contrib.driver.lps25h.Lps25h;
import com.google.android.things.contrib.driver.sensehat.Joystick;
import com.google.android.things.contrib.driver.sensehat.LedMatrix;
import com.google.android.things.contrib.driver.sensehat.SenseHat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.justif.iotsensehat.cloud.CloudPublisherService;

import static com.google.android.things.contrib.driver.sensehat.Joystick.OnButtonEventListener;

/**
 * To use this service, start it from your component (like an activity):
 * <pre>{@code
 * this.startService(new Intent(this, TemperaturePressureService.class))
 * }</pre>
 */
public class SenseHatService extends Service {
    private static final String TAG = SenseHatService.class.getSimpleName();
    private static final long SENSOR_READ_INTERVAL_MS = TimeUnit.SECONDS.toMillis(20);
    public static final String SENSOR_TYPE_JOYSTICK = "joystick_";
    public static final String SENSOR_TYPE_TEMPERATURE_DETECTION = "temperature";
    public static final String SENSOR_TYPE_TEMPERATURE2_DETECTION = "temperature2";
    public static final String SENSOR_TYPE_AMBIENT_PRESSURE_DETECTION = "ambient_pressure";
    public static final String SENSOR_TYPE_HUMIDITY_DETECTION = "humidity";
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
    private Looper mSensorLooper;

    private SenseHat mSenseHat;
    private Joystick mJoystick;
    private LedMatrix mLedMatrix;
    private Lps25h mEnvironmentalSensor;
    private Hts221 mHumiditySensor;

    @Override
    public void onCreate() {
        Log.i(TAG, "Creating SenseHatService");
        initializeServiceIfNeeded();
        setupSenseHat();


        // Start the thread that collects sensor data
        HandlerThread thread = new HandlerThread("CloudPublisherService");
        thread.start();
        mSensorLooper = thread.getLooper();

        final Handler sensorHandler = new Handler(mSensorLooper);
        sensorHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    initializeServiceIfNeeded();
                    //connectToAvailableSensors();
                    collectContinuousSensors();
                } catch (Throwable t) {
                    Log.e(TAG, String.format(Locale.getDefault(),
                            "Cannot collect sensor data, will try again in %d ms",
                            SENSOR_READ_INTERVAL_MS), t);
                }
                sensorHandler.postDelayed(this, SENSOR_READ_INTERVAL_MS);
            }
        }, SENSOR_READ_INTERVAL_MS);
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
                    collectSensorOnChange(SENSOR_TYPE_JOYSTICK + key, pressed ? 1 : 0);
                }});
            mLedMatrix = mSenseHat.openDisplay();
            mLedMatrix.draw(0);
            mEnvironmentalSensor = mSenseHat.openPressureSensor();
            mHumiditySensor = mSenseHat.openHumiditySensor();
        } catch (IOException e) {
            Log.e(TAG, "Error configuring sensor", e);
        }
    }

    private void destroySenseHat() {
        if (mSenseHat != null) {
            try {
                mEnvironmentalSensor.close();
                mHumiditySensor.close();
                mSenseHat.close();

            } catch (IOException e) {
                Log.e(TAG, "Error closing sensor", e);
            } finally {
                mEnvironmentalSensor = null;
                mHumiditySensor = null;
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

    private void collectContinuousSensors() {
        if (mPublishService != null) {
            List<SensorData> sensorsData = new ArrayList<>();
            addLps25hReadings(sensorsData);
            addHts221Readings(sensorsData);
            Log.d(TAG, "collected continuous sensor data: " + sensorsData);
            mPublishService.logSensorData(sensorsData);
        }
    }

    private void addLps25hReadings(List<SensorData> output) {
        if (mEnvironmentalSensor != null) {
            try {
                long now = System.currentTimeMillis();
                if (now >= INITIAL_VALID_TIMESTAMP) {
                    float pressure = mEnvironmentalSensor.readPressure();
                    float temperature = mEnvironmentalSensor.readTemperature();
                    output.add(new SensorData(now, SENSOR_TYPE_AMBIENT_PRESSURE_DETECTION, pressure));
                    output.add(new SensorData(now, SENSOR_TYPE_TEMPERATURE_DETECTION,
                            temperature));
                } else {
                    Log.i(TAG, "Ignoring sensor readings because timestamp is invalid. " +
                            "Please, set the device's date/time");
                }
            } catch (Throwable t) {
                Log.w(TAG, "Cannot collect Bmx280 data. Ignoring it for now", t);
                //closeBmx280Quietly();
            }
        }
    }

    private void addHts221Readings(List<SensorData> output) {
        if (mHumiditySensor != null) {
            try {
                long now = System.currentTimeMillis();
                if (now >= INITIAL_VALID_TIMESTAMP) {
                    float humidity = mHumiditySensor.readHumidity();
                    float temperature = mHumiditySensor.readTemperature();
                    output.add(new SensorData(now, SENSOR_TYPE_HUMIDITY_DETECTION, humidity));
                    output.add(new SensorData(now, SENSOR_TYPE_TEMPERATURE2_DETECTION,
                            temperature));
                } else {
                    Log.i(TAG, "Ignoring sensor readings because timestamp is invalid. " +
                            "Please, set the device's date/time");
                }
            } catch (Throwable t) {
                Log.w(TAG, "Cannot collect Bmx280 data. Ignoring it for now", t);
                //closeBmx280Quietly();
            }
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
