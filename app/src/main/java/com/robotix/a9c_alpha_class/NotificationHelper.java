package com.robotix.a9c_alpha_class;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static com.robotix.a9c_alpha_class.BaseData.*;
import static com.robotix.a9c_alpha_class.MainActivity.context;
import static java.time.temporal.ChronoUnit.DAYS;

public class NotificationHelper extends Service {

    private Context mContext;
    private static final String NOTIFICATION_CHANNEL_ID = "10001";
    private static final int DEZHURSTVA_NOTIFY_ID = 100114;
    private static final int SERVER_EVENTS_NOTIFY_ID = 100115;
    private static final int OLYMPS_NOTIFY_ID = 100116;

    NotificationHelper(Context context) {
        mContext = context;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        createNotification();
        return null;
    }

    void createNotification() {
        try {
            CharSequence name = "ROBOTIX_NOTIFICATION";
            String description = "description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after context
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
            notificationManager.createNotificationChannel(channel);
            System.out.println("------------------------------------------------Notification!------------------------------");

            NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.vector_icon)
                    .setOnlyAlertOnce(true);

            SharedPreferences settings = mContext.getSharedPreferences(MainActivity.PREFS_NAME, 0);

            int uid = settings.getInt("uid", 0) - 1;
            if (uid < 0) {
                Intent intent = new Intent(mContext, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
                builder.setContentTitle("Пользователь не выбран")
                        .setContentText("Выберите в приложении кто вы")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setOngoing(true)
                        .setContentIntent(pendingIntent);
                notificationManager.notify(DEZHURSTVA_NOTIFY_ID, builder.build());
            } else {
                LocalDate date = LocalDate.now();
                long day_number = DAYS.between(LocalDate.of(date.getYear() - 1, 12, 31), date);
                if (dezhurstva[Integer.parseInt(String.valueOf(day_number)) % (dezhurstva.length / 2) * 2] == uid || dezhurstva[Integer.valueOf(String.valueOf(day_number)) % (dezhurstva.length / 2) * 2 + 1] == uid) {

                    builder.setContentTitle("Вы - дежурный!")
                            .setContentText(students[uid] + ", сегодня вы дежурите, не забывайте об этом")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setOngoing(true);
                    notificationManager.notify(DEZHURSTVA_NOTIFY_ID, builder.build());
                } else {
                    notificationManager.cancel(DEZHURSTVA_NOTIFY_ID);
                }
                if (olympDates().keySet().contains(date.toString())) {
                    for (int i = 0; i < subjects.length; i++) {
                        if (Arrays.asList(olympDates().get(date.toString())).contains(i) && Arrays.asList(studentsOnOlymp[i]).contains(uid)) {
                            Intent intent = new Intent(mContext, ResultActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);

                            builder.setContentTitle("Олимпиада")
                                    .setContentText("Не забудьте что сегодня олимпиада: " + subjects[i])
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setOngoing(true)
                                    .setContentIntent(pendingIntent);
                            notificationManager.notify(OLYMPS_NOTIFY_ID + i, builder.build());
                        } else {
                            notificationManager.cancel(OLYMPS_NOTIFY_ID + i);
                        }
                    }
                }
                SharedPreferences serverEventStorage = context.getSharedPreferences(MainActivity.SERVER_EVENTS_DATA, Context.MODE_PRIVATE);
                Map<String, String> serverEvents = (Map<String, String>) serverEventStorage.getAll();
                int maxPriority = -1;
                for (String key : serverEvents.keySet()) {
                    try {
                        ArrayList<Object> event = (ArrayList<Object>) ObjectSerializer.deserialize(serverEvents.get(key));
                        if (event.get(0).equals(date.toString()) && Arrays.asList(event.get(5).toString().replaceAll(" ", "").split("-")).contains(Integer.toString(uid)) && Integer.parseInt((String) event.get(4)) > maxPriority) {
                            Intent intent = new Intent(mContext, ResultActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
                            builder.setContentTitle("Событие")
                                    .setContentText((String) event.get(3))
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setOngoing(true)
                                    .setContentIntent(pendingIntent);
                            notificationManager.notify(SERVER_EVENTS_NOTIFY_ID, builder.build());
                            maxPriority = Integer.parseInt((String) event.get(4));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (maxPriority == -1) notificationManager.cancel(SERVER_EVENTS_NOTIFY_ID);
            }
            assert notificationManager != null;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}