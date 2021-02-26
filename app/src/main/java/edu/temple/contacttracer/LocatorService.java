package edu.temple.contacttracer;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import edu.temple.contacttracer.database.ContactUUIDModel;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

// https://developer.android.com/guide/components/services
public class LocatorService extends Service {

    private static final String ENDPOINT = "https://kamorris.com/lab/ct_tracking.php";
    LocationManager locationManager;
    LocationListener locationListener;

    String latitude;
    String longitude;
    String begin;
    String end;

    String payload;

    Location prevLocation;
    boolean isCountdown;

    /**
     *
     * TODO:
     *      1. change time to milliseconds
     *      2. filter own uuid and check for 6 foot distance
     *      3. store locally
     */

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        isCountdown = false;
        locationManager = getSystemService(LocationManager.class);

        final CountDownTimer countDownTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long l) {
                isCountdown = true;
            }

            /**
             * create a Debug warning if the timer reaches 60 seconds...
             */
            @Override
            public void onFinish() {
                Log.d("stationaryTimer", "YOU HAVE STAYED IN A NEW LOCATION FOR MORE THAN 60 SECONDS");
                isCountdown = false;
                end = String.valueOf(Calendar.getInstance().getTimeInMillis());

                List <ContactUUIDModel> contactUUIDModelList = MainActivity.getContactModelList(getApplicationContext());
                if (contactUUIDModelList.size() > 0) {
                    String mostRecentUUID = contactUUIDModelList.get(0).uuid;
                    sendSedentaryEvent(mostRecentUUID, latitude, longitude, begin, end);
                };
            }

        };

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {

                if (prevLocation == null){
                    prevLocation = location;
                }

                if (location.distanceTo(prevLocation) >= 10.0) {
                    showNotification();
                    // restart the countDownTimer
                    if (isCountdown){
                        countDownTimer.cancel();
                        isCountdown = false;
                    }
                    latitude = String.valueOf(location.getLatitude());
                    longitude = String.valueOf(location.getLongitude());
                    Log.i("latitude", "onLocationChanged: " + latitude);
                    Log.i("longitude", "onLocationChanged: " + longitude);

                    countDownTimer.start();
                    begin = String.valueOf(Calendar.getInstance().getTimeInMillis());
                    prevLocation = location;
                }
            }
        };



        FirebaseMessaging.getInstance().subscribeToTopic("TRACKING").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                String msg = task.isSuccessful()? "success": "failed";
                Log.d("subscribeMessage", msg);
            }
        });




    }

    // https://stackoverflow.com/questions/51587863/bad-notification-for-start-foreground-invalid-channel-for-service-notification
    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 1, locationListener);
        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 1, locationListener);

        return super.onStartCommand(intent, flags, startId);
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(locationListener);
    }

    public void showNotification(){

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);


        String channelID = "default";
        String channelName = "Foreground Service Channel";
        NotificationChannel notificationChannel = new NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_NONE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(notificationChannel);


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Location change")
                .setContentText("you have moved 10 meters from your last location")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setPriority(NotificationManager.IMPORTANCE_MAX)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();

        startForeground(2, notification);
    }


    public String uuid;
    private String lat;
    private String longt;
    private String sedentary_begin;
    private String sedentary_end;

    public String sendSedentaryEvent(String uid, final String latitude, final String longitude, String sed_begin, final String sed_end){
        uuid = uid;
        this.lat = latitude;
        this.longt = longitude;
        this.sedentary_begin = sed_begin;
        this.sedentary_end = sed_end;
        final String[] output = {""};
        final String body = "{ 'uuid': '" + uuid + "'" +
                ", 'latitude': '" + latitude + "'" +
                ", 'longitude': '" + longitude + "'" +
                ", 'sedentary_begin': '" + sedentary_begin + "'" +
                ", 'sedentary_end': " + sedentary_end + "' }";
        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();

                try {

                    RequestBody formBody = new FormBody.Builder()
                            .add("uuid", uuid)
                            .add("latitude", lat)
                            .add("longitude", longt)
                            .add("sedentary_begin", sedentary_begin)
                            .add("sedentary_end", sedentary_end)
                            .build();

                    Request request = new Request.Builder()
                            .url(ENDPOINT)
                            .post(formBody)
                            .build();

                    OkHttpClient httpClient = new OkHttpClient();
                    try(Response response = httpClient.newCall(request).execute()){

                        output[0] = response.body().string();
                        Log.i("RESPONSE", "response: " + output[0]);
                    } catch (Exception e){
                        e.printStackTrace();
                        Log.i("CONNECTION", "SendSedentaryEvent: CONNECTION NOT ESTABLISHED");
                        output[0] = null;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    output[0] = null;
                }
            }
        };

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return output[0];

    }


}
