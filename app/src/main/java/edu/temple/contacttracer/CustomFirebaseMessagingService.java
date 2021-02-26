package edu.temple.contacttracer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.temple.contacttracer.database.ContactUUIDModel;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class CustomFirebaseMessagingService extends FirebaseMessagingService {

    String payload;



    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();

    }

    public CustomFirebaseMessagingService() {
    }



    @SuppressLint("MissingPermission")
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        payload = remoteMessage.getData().get("payload");
        Log.i("PAYLOAD", "onMessageReceived: " + payload);

        Intent intent = new Intent("FMS");
        intent.putExtra("payload", payload);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }

    @Override
    public void onNewToken(@NonNull @NotNull String s) {
        super.onNewToken(s);

    }


}
