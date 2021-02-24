package edu.temple.contacttracer;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class CustomFirebaseMessagingService extends FirebaseMessagingService {

    String payload;

    public CustomFirebaseMessagingService(){

    }
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        payload = remoteMessage.getData().get("payload");
        Log.i("PAYLOAD", "onMessageReceived: " + payload);
        super.onMessageReceived(remoteMessage);
    }
}
