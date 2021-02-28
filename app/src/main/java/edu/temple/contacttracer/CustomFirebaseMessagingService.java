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


/**
 * Singleton service implemented by OS
 */
public class CustomFirebaseMessagingService extends FirebaseMessagingService {

    String payload;



    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();

    }

    public CustomFirebaseMessagingService() {
    }


    /**
     * extract json string from remote message and send to locator service
     * @param remoteMessage payload
     */
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
