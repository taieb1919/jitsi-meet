/*
 * Copyright @ 2017-present 8x8, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.softic.wmeet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionEntry;
import android.content.RestrictionsManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.Nullable;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;

import org.jitsi.meet.sdk.JitsiMeet;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

/**
 * The one and only Activity that the Jitsi Meet app needs. The
 * {@code Activity} is launched in {@code singleTask} mode, so it will be
 * created upon application initialization and there will be a single instance
 * of it. Further attempts at launching the application once it was already
 * launched will result in {@link MainActivity#onNewIntent(Intent)} being called.
 */
public class MainActivity extends JitsiMeetActivity {
    /**
     * The request code identifying requests for the permission to draw on top
     * of other apps. The value must be 16-bit and is arbitrarily chosen here.
     */
    private static final int OVERLAY_PERMISSION_REQUEST_CODE
        = (int) (Math.random() * Short.MAX_VALUE);

    /**
     * ServerURL configuration key for restriction configuration using {@link android.content.RestrictionsManager}
     */
    public static final String RESTRICTION_SERVER_URL = "SERVER_URL";

    /**
     * Broadcast receiver for restrictions handling
     */
    private BroadcastReceiver broadcastReceiver;

    /**
     * Flag if configuration is provided by RestrictionManager
     */
    private boolean configurationByRestrictions = false;

    /**
     * Default URL as could be obtained from RestrictionManager
     */
    private String defaultURL;
    public static String LinkConf = "";

    // JitsiMeetActivity overrides
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        JitsiMeet.showSplashScreen(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected boolean extraInitialize() {
        Log.d(this.getClass().getSimpleName(), "LIBRE_BUILD=" + com.softic.wmeet.BuildConfig.LIBRE_BUILD);

        // Setup Crashlytics and Firebase Dynamic Links
        // Here we are using reflection since it may have been disabled at compile time.
        try {
            Class<?> cls = Class.forName("com.softic.wmeet.GoogleServicesHelper");
            Method m = cls.getMethod("initialize", JitsiMeetActivity.class);
            m.invoke(null, this);
        } catch (Exception e) {
            // Ignore any error, the module is not compiled when LIBRE_BUILD is enabled.
        }

        // In Debug builds React needs permission to write over other apps in
        // order to display the warning and error overlays.

       
        if (BuildConfig.DEBUG) {
            if (!Settings.canDrawOverlays(this)) {

                Intent intent
                    = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));

                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);

                return true;
            }
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getIntentExtra();
        AndroidNetworking.initialize(getApplicationContext());
    }

    @Override
    public void onNewIntent(Intent intent) {
        getIntentExtra();

        super.onNewIntent(intent);

    }

    private String resId = "";
    private String uid = "";
    private String i_id = "";
    private String refIntetnt = "";

    private void getIntentExtra() {
        Bundle extras = getIntent().getExtras();
        String _keyextras = "";
        if (extras != null) {
            for (String key : extras.keySet()) {
                if (key.toLowerCase().contains("extra.referrer")) {
                    _keyextras = key;
                }
                refIntetnt += key + " , ";
            }
      //      ShowAlert("getIntentExtra", extras.toString());
            if (_keyextras.length() == 0) {

                return;
            }


         //   ShowAlert("", refIntetnt);

            refIntetnt = extras.get(_keyextras).toString();

            if (refIntetnt == null)
                return;
            Uri uri = Uri.parse(refIntetnt);
            resId = uri.getQueryParameter("ResId");
            uid = uri.getQueryParameter("Uid");
            i_id = uri.getQueryParameter("Id");

        }


    }

    @Override
    protected void initialize() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // As new restrictions including server URL are received,
                // conference should be restarted with new configuration.


                leave();
            //    getIntentExtra();
                recreate();
            }
        };
        registerReceiver(broadcastReceiver,
            new IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED));

        resolveRestrictions();
        setJitsiMeetConferenceDefaultOptions();
       // getIntentExtra();
        super.initialize();
    }

    @Override
    public void onDestroy() {
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
        UpdateInformationInServer(false);
        super.onDestroy();
    }

    private void setJitsiMeetConferenceDefaultOptions() {
        // Set default options
        JitsiMeetConferenceOptions defaultOptions
            = new JitsiMeetConferenceOptions.Builder()
            .setWelcomePageEnabled(true)
            .setServerURL(buildURL(defaultURL))
            .setFeatureFlag("call-integration.enabled", false)
            .setFeatureFlag("resolution", 360)
            .setFeatureFlag("server-url-change.enabled", !configurationByRestrictions)
            //.setFeatureFlag("server-url-change.enabled", configurationByRestrictions)

            .setFeatureFlag("meeting-password.enabled", false)
            .setFeatureFlag("close-captions.enabled", false)


            .setFeatureFlag("add-people.enabled", false)
            .setFeatureFlag("invite.enabled", false)
            .setFeatureFlag("live-streaming.enabled", false)


            .build();
        JitsiMeet.setDefaultConferenceOptions(defaultOptions);
    }

    private void resolveRestrictions() {
        RestrictionsManager manager =
            (RestrictionsManager) getSystemService(Context.RESTRICTIONS_SERVICE);
        Bundle restrictions = manager.getApplicationRestrictions();
        Collection<RestrictionEntry> entries = manager.getManifestRestrictions(
            getApplicationContext().getPackageName());
        for (RestrictionEntry restrictionEntry : entries) {
            String key = restrictionEntry.getKey();
            if (RESTRICTION_SERVER_URL.equals(key)) {
                // If restrictions are passed to the application.
                if (restrictions != null &&
                    restrictions.containsKey(RESTRICTION_SERVER_URL)) {
                    defaultURL = restrictions.getString(RESTRICTION_SERVER_URL);
                    configurationByRestrictions = true;
                    // Otherwise use default URL from app-restrictions.xml.
                } else {
                    defaultURL = restrictionEntry.getSelectedString();
                    configurationByRestrictions = false;
                }
            }
        }
    }

    private void UpdateInformationInServer(boolean IsJoined) {

        if (resId==null&&i_id==null&&uid==null)
            return;
        ANRequest.GetRequestBuilder aa = AndroidNetworking.get("https://cnn.eppm.com.tn/SofticWMeet/API/WmeetApi/LogParticipant");

        aa.addHeaders("IsWmeetMobile", "true");

        if (i_id != null) aa.addQueryParameter("id", i_id);
        else aa.addQueryParameter("id", "");

        if (resId != null) aa.addQueryParameter("resid", resId);
        else aa.addQueryParameter("resid", "");

        if (uid != null) aa.addQueryParameter("uid", uid);
        else aa.addQueryParameter("uid", "");

        aa.addQueryParameter("left", (String.valueOf(!IsJoined)));
       ANRequest rq= aa.build();
    //    ShowAlert("ANRequest",rq.getUrl());
        //aa.build()
          rq  .getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    //ShowAlert("res","Ok  net "+response.toString());
                }

                @Override
                public void onError(ANError error) {
                    // ShowAlert("error  net","error  net "+error.getResponse());
                }
            });

    }


    @Override
    public void onConferenceJoined(Map<String, Object> data) {
        // super.onConferenceJoined(data);
        UpdateInformationInServer(true);

    }

    @Override
    public void onConferenceTerminated(Map<String, Object> data) {
        //super.onConferenceTerminated(data);
        UpdateInformationInServer(false);

/*
        resId=null;
        i_id=null;
        uid=null;*/
    }

    public void ShowAlert(String title, String msg) {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        alertDialog.show();
    }

    // Activity lifecycle method overrides
    //

    @Override
    public void onActivityResult(int requestCode,   int resultCode, Intent data) {

        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                initialize();
                return;
            }

            throw new RuntimeException("Overlay permission is required when running in Debug mode.");
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    // ReactAndroid/src/main/java/com/facebook/react/ReactActivity.java
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (com.softic.wmeet.BuildConfig.DEBUG && keyCode == KeyEvent.KEYCODE_MENU) {
            JitsiMeet.showDevOptions();
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);

        Log.d(TAG, "Is in picture-in-picture mode: " + isInPictureInPictureMode);

        if (!isInPictureInPictureMode) {
            this.startActivity(new Intent(this, getClass())
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        }
    }

    // Helper methods
    //

    private @Nullable
    URL buildURL(String urlStr) {
        try {

            return new URL(urlStr);
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
