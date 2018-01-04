package de.justif.iotsensehat;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by simon on 04.01.18.
 */

public class IoTCoreService extends Service {
    private static final String TAG = IoTCoreService.class.getSimpleName();

    @Override
    public void onCreate() {
        Log.i(TAG, "Creating SenseHatService");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
