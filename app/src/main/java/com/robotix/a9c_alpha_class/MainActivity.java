package com.robotix.a9c_alpha_class;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static androidx.core.content.FileProvider.getUriForFile;

public class MainActivity extends AppCompatActivity {
    public static Boolean debug = false;

    private static int currentTab = 0;
    protected static String PREFS_NAME = "com.robotix.a9c_alpha_class";
    protected static String SERVER_EVENTS_DATA = "server-events-data";
    protected static String USER_EVENTS_DATA = "server-events-data";

    protected static final int INSTALL_PACKAGES_REQUESTCODE = 10011;
    protected static final int GET_UNKNOWN_APP_SOURCES = 10012;
    protected static final int WRITE_FILES_REQUESTCODE = 10013;
    protected static final String NOTIFICATION_CHANNEL_ID = "ROBOTIX_NOTIFICATION";

    protected static Context context;

    protected static String selectedDate = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1).toString();

    void setCurrentTab(int tab) {
        currentTab = tab;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = super.getApplicationContext();

        // Events Update
        try {
            (new RequestHandler(RequestHandler.EVENTS_UPDATE_MODE, MainActivity.this)).start();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Notifications
        try {
            NotificationHelper notificationHelper = new NotificationHelper(this);
            notificationHelper.createNotification();
            myAlarm(0, this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        setCurrentTab(0);

        // Update
        try {
            registerReceiver(attachmentDownloadCompleteReceive, new IntentFilter(
                    DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            updateIfAvailable();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // UI
        setContentView(R.layout.activity_main);
        try {
            // Получаем ViewPager и устанавливаем в него адаптер
            ViewPager viewPager = findViewById(R.id.viewpager);
            viewPager.setAdapter(
                    new SampleFragmentPagerAdapter(getSupportFragmentManager(), MainActivity.this));
            viewPager.setCurrentItem(currentTab);

            // Передаём ViewPager в TabLayout
            TabLayout tabLayout = findViewById(R.id.sliding_tabs);
            tabLayout.setupWithViewPager(viewPager);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        myAlarm(5, this);
        super.onDestroy();
    }

    BroadcastReceiver attachmentDownloadCompleteReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(
                        DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                openDownloadedAttachment(context, downloadId);
            }
        }
    };

    private void openDownloadedAttachment(final Context context, final long downloadId) {
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);
        if (cursor.moveToFirst()) {
            int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            String downloadLocalUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            String downloadMimeType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));
            if ((downloadStatus == DownloadManager.STATUS_SUCCESSFUL) && downloadLocalUri != null) {
                File file = new File(Uri.parse(downloadLocalUri).getPath());
                if (downloadMimeType.equals("application/vnd.android.package-archive")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri contentUri = getUriForFile(context, PREFS_NAME + ".fileprovider", file);
                    intent.setDataAndType(contentUri, "application/vnd.android.package-archive");

                    List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }

                    startActivity(intent);
                }
            }
        }
        cursor.close();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case INSTALL_PACKAGES_REQUESTCODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {// Если это разрешение уже имеет, установить или перейти на интерфейс лицензии
                    installApk(this);
                } else {
                    Uri packageURI = Uri.parse("package:" + getPackageName()); // получить имя пакета, напрямую перейти к соответствующему интерфейсу авторизации приложений
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
                    startActivityForResult(intent, GET_UNKNOWN_APP_SOURCES);
                }
                break;
            case WRITE_FILES_REQUESTCODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {// Если это разрешение уже имеет, установить или перейти на интерфейс лицензии
                    (new RequestHandler(RequestHandler.CHECK_APP_UPDATE_MODE, MainActivity.this)).start();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_FILES_REQUESTCODE);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //8.0 Сильный интерфейс авторизации обновления
        switch (requestCode) {
            case GET_UNKNOWN_APP_SOURCES:
                if (getPackageManager().canRequestPackageInstalls()) {}
                else {
                    ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.REQUEST_INSTALL_PACKAGES}, INSTALL_PACKAGES_REQUESTCODE);
                }
                break;
            default:
                break;
        }

    }

    void updateIfAvailable() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_FILES_REQUESTCODE);
        }
        else {
            (new RequestHandler(RequestHandler.CHECK_APP_UPDATE_MODE, MainActivity.this)).start();
        }
    }

    protected static void installApk(Context context) {
        DownloadManager downloadmanager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri_up = Uri.parse("https://robotix-com.web.app/docs/app-" + (debug ? "debug" : "9c") + ".apk");

        DownloadManager.Request request_up = new DownloadManager.Request(uri_up);
        request_up.setTitle("9C-app");
        request_up.setDescription("Download update");
        request_up.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        request_up.setVisibleInDownloadsUi(false);
        request_up.setDestinationInExternalFilesDir(context, "Download", "app-9c.apk");
        downloadmanager.enqueue(request_up);
    }

    protected static void saveEvents(ArrayList<Object>[] events, String to, Boolean clear, Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int lastId = clear == true ? 0 : settings.getInt("last-" + to + "-index", 0);
        SharedPreferences eventsStorage = context.getSharedPreferences(to, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = eventsStorage.edit();
        if (clear) editor.clear();
        SharedPreferences.Editor settingsEditor = eventsStorage.edit();
        try {
            for (ArrayList<Object> event : events) {
                lastId += 1;
                editor.putString("event" + lastId, ObjectSerializer.serialize(event));
            }
            settingsEditor.putInt("last-" + to + "-index", lastId);
            settingsEditor.apply();
            editor.apply();
        } catch (IOException e) {
            e.printStackTrace();
        }
        settingsEditor.commit();
        editor.commit();
        checkForOutDated();
    }

    protected static void checkForOutDated() {
        SharedPreferences eventsStorage = context.getSharedPreferences(SERVER_EVENTS_DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = eventsStorage.edit();
        Map<String, String> serverEvents = (Map<String, String>) eventsStorage.getAll();
        for (String key : serverEvents.keySet()) {
            try {
                ArrayList<Object> event = ((ArrayList<Object>) ObjectSerializer.deserialize(serverEvents.get(key)));
                if (LocalDate.parse(event.get(0).toString()).plusDays(Duration.of(Long.parseLong(event.get(1).toString()),
                        ChronoUnit.MINUTES).toDays()).compareTo(LocalDate.now()) < 0) {
                    editor.remove(key);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        editor.commit();
    }

    public static ArrayList<Object>[] createEventObjectsFromString(String eventsAsString) {
        eventsAsString = eventsAsString.replaceAll("\n", "");
        eventsAsString = eventsAsString.replaceAll(" {2,}", "");
        eventsAsString = eventsAsString.replaceAll(", ", ",");
        String[] rows = eventsAsString.split(";");
        ArrayList<Object>[] result = new ArrayList[rows.length];
        for (int i = 0; i < rows.length; i++) {
            result[i] = new ArrayList<>(Arrays.asList(rows[i].split(",")));
        }
        return result;
    }

    static public void myAlarm(int after, Context context) {

        Calendar calendar = Calendar.getInstance();

        if (after != 0) {
            calendar.add(Calendar.SECOND, after);
        }
        else {
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
        }

        if (calendar.getTime().compareTo(new Date()) < 0)
            calendar.add(Calendar.DAY_OF_MONTH, 1);

        Intent intent = new Intent(context.getApplicationContext(), Restarter.class);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), after, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        if (alarmManager != null) {
            if (after == 0) {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY / 24, pendingIntent);
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
            else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }

        }

    }

}

