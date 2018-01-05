package de.justif.iotsensehat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Starting MainActivity");
        startServices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopServices();
    }



    private void startServices() {

        this.startService(new Intent(this, SenseHatService.class));

    }

    private void stopServices() {

        this.stopService(new Intent(this, SenseHatService.class));
    }



}
