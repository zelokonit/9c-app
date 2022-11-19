package com.robotix.a9c_alpha_class;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Restarter extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            MainActivity.myAlarm(0, context);
            NotificationHelper notificationHelper = new NotificationHelper(context);
            notificationHelper.createNotification();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}



/*
свет
марионетки
день рождения
ты или я
*/