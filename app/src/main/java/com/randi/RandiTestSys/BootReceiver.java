package com.randi.RandiTestSys;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.window.SplashScreen;

import androidx.annotation.RequiresApi;

public class BootReceiver extends BroadcastReceiver {
    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override

    public void onReceive(Context context, Intent intent) {

        Intent myIntent = new Intent(context, SplashScreen.class);

        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(myIntent);

    }

}
