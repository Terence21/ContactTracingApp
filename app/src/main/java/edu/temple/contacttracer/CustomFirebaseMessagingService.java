package edu.temple.contacttracer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import org.jetbrains.annotations.NotNull;

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
