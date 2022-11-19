package com.robotix.a9c_alpha_class;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;

import static com.robotix.a9c_alpha_class.MainActivity.INSTALL_PACKAGES_REQUESTCODE;
import static com.robotix.a9c_alpha_class.MainActivity.PREFS_NAME;
import static com.robotix.a9c_alpha_class.MainActivity.SERVER_EVENTS_DATA;
import static com.robotix.a9c_alpha_class.MainActivity.debug;


class RequestHandler extends Thread {

    protected static final int CHECK_APP_UPDATE_MODE = 1;
    protected static final int EVENTS_UPDATE_MODE = 2;

    int mode;
    Context context;

    RequestHandler(int start_on_mode, Context context) {
        mode = start_on_mode;
        this.context = context;
    }

    @Override
    public void run() {
        try {
            switch (mode) {
                case CHECK_APP_UPDATE_MODE: {
                    String text = requestText("https://robotix-com.web.app/docs/app" + (debug ? "-debug" : "") + "-version.txt");
                    if (text.compareTo(BuildConfig.VERSION_NAME) > 0) {
                        if (context.getPackageManager().canRequestPackageInstalls()) {
                            ((AppCompatActivity) context).runOnUiThread(() -> {
                                MainActivity.installApk(context);
                            });
                        } else {
                            ActivityCompat.requestPermissions((AppCompatActivity) context, new String[]{Manifest.permission.REQUEST_INSTALL_PACKAGES}, INSTALL_PACKAGES_REQUESTCODE);
                        }
                    }
                    break;
                }
                case EVENTS_UPDATE_MODE: {
                    checkForNewEvents();
                    break;
                }
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }

    protected void checkForNewEvents() {
        int serverEventListVersion;
        try {
            serverEventListVersion = Integer.parseInt(requestText("https://robotix-com.web.app/apps/a9c_alpha_class/event-list-version.txt").split("\n")[0]);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        SharedPreferences settings = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        if (settings.getInt("lastEventCheck", 0) < serverEventListVersion) {
            String eventsAsString;
            try {
                eventsAsString = requestText("https://robotix-com.web.app/apps/a9c_alpha_class/events-list.txt");
            }
            catch (Exception e) {
                e.printStackTrace();
                return;
            }
            MainActivity.saveEvents(MainActivity.createEventObjectsFromString(eventsAsString), SERVER_EVENTS_DATA, true, context);
            ((AppCompatActivity) context).runOnUiThread(() -> {
                ExpandableListView vp = ((AppCompatActivity) context).findViewById(R.id.eventExpandableListView);
                if (vp != null) {
                    ((SimpleExpandableListAdapter) vp.getExpandableListAdapter()).notifyDataSetChanged();
                }
                vp = ((AppCompatActivity) context).findViewById(R.id.scheduleExpandableListView);
                if (vp != null) {
                    ((SimpleExpandableListAdapter) vp.getExpandableListAdapter()).notifyDataSetChanged();
                }
            });
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt("lastEventCheck", serverEventListVersion);
            editor.apply();
        }
    }

    public static String requestText(String urlAsString) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlAsString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            for (String line; (line = reader.readLine()) != null; ) {
                result.append(line);
            }
        }
        return String.join("\n", result);
    }
}
