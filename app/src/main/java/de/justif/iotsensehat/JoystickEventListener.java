package de.justif.iotsensehat;


import android.util.Log;

import com.google.android.things.contrib.driver.sensehat.Joystick;

/**
 * Created by simon on 04.01.18.
 */

class JoystickEventListener implements Joystick.OnButtonEventListener {
    private static final String TAG = JoystickEventListener.class.getSimpleName();

    @Override
    public void onButtonEvent(int key, boolean pressed) {
        Log.i(TAG, "key: " + key + "pressed" + pressed);
    }
}